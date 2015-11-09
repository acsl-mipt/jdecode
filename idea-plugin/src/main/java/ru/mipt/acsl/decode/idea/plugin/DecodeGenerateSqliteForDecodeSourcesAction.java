package ru.mipt.acsl.decode.idea.plugin;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Resources;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.psi.ClassFileViewProvider;
import com.intellij.psi.PsiManager;
import ru.mipt.acsl.decode.model.domain.proxy.DecodeResolvingResult;
import ru.mipt.acsl.decode.parser.psi.DecodeFile;
import ru.mipt.acsl.decode.model.exporter.ModelExportingException;
import ru.mipt.acsl.decode.model.exporter.DecodeSqlite3Exporter;
import ru.mipt.acsl.decode.model.exporter.DecodeSqlite3ExporterConfiguration;
import ru.mipt.acsl.decode.model.domain.DecodeReferenceable;
import ru.mipt.acsl.decode.model.domain.DecodeRegistry;
import ru.mipt.acsl.decode.model.domain.impl.SimpleDecodeRegistry;
import ru.mipt.acsl.decode.modeling.TransformationResult;
import ru.mipt.acsl.decode.parser.DecodeFileType;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * @author Artem Shein
 */
public class DecodeGenerateSqliteForDecodeSourcesAction extends AnAction
{

    public static final String GROUP_DISPLAY_ID = "Decode SQLite Generation";

    @Override
    public void actionPerformed(AnActionEvent anActionEvent)
    {
        Project project = anActionEvent.getProject();
        if (project == null)
        {
            return;
        }
        PsiManager psiManager = PsiManager.getInstance(project);
        DecodeRegistry registry = SimpleDecodeRegistry.newInstance();
        TransformationResult<DecodeRegistry> result = new DecodeTransformationResult(registry);
        ProjectRootManager.getInstance(project).getFileIndex().iterateContent(virtualFile -> {
            if (virtualFile.getFileType().equals(
                    DecodeFileType.INSTANCE))
            {
                DecodeFile file = new DecodeFile(new ClassFileViewProvider(psiManager, virtualFile));
                new DecodeFileProcessor(registry, result).process(file);
            }
            return true;
        });
        result.getMessages().forEach(DecodeFileProcessor::notifyUser);
        if (!result.hasError() && result.getResult().isPresent())
        {
            Preconditions.checkState(result.getResult().get() == registry);
            DecodeResolvingResult<DecodeReferenceable> resolvingResult = SimpleDecodeDomainModelResolver.newInstance().resolve(
                    registry);
            if (resolvingResult.hasError())
            {
                resolvingResult.getMessages()
                        .forEach(DecodeFileProcessor::notifyUser);
            }
        }
        FileSaverDialog fileChooserDialog = FileChooserFactory.getInstance().createSaveFileDialog(
                new FileSaverDescriptor("Save file to", "", "sqlite"), (Project) null);
        VirtualFileWrapper fileWrapper = fileChooserDialog.save(project.getBaseDir(), "decode.sqlite");
        if (fileWrapper == null)
        {
            return;
        }
        VirtualFile sqliteVirtualFile = fileWrapper.getVirtualFile(true);
        if (sqliteVirtualFile == null)
        {
            return;
        }
        File sqliteFile = VfsUtil.virtualToIoFile(sqliteVirtualFile);
        if (sqliteFile.exists())
        {
            if (!sqliteFile.delete())
            {
                Notifications.Bus.notify(new Notification(GROUP_DISPLAY_ID, "Can't delete file",
                        String.format("File '%s' can't be deleted", sqliteFile.getAbsolutePath()),
                        NotificationType.ERROR));
            }
        }
        DecodeSqlite3ExporterConfiguration config = new DecodeSqlite3ExporterConfiguration();
        config.setOutputFile(sqliteFile);
        try
        {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException e)
        {
            throw new ModelExportingException(e);
        }
        try(Connection connection = DriverManager
                .getConnection("jdbc:sqlite:" + config.getOutputFile().getAbsolutePath()))
        {
            connection.setAutoCommit(false);
            for (String sql : Resources.toString(Resources.getResource(this.getClass(), "/edu/phystech/ru/mipt/acsl/decode/decode.sql"), Charsets.UTF_8).split(
                    Pattern.quote(";")))
            {
                connection.prepareStatement(sql).execute();
            }
            connection.commit();
        }
        catch (SQLException | IOException e)
        {
            throw new ModelExportingException(e);
        }
        new DecodeSqlite3Exporter(config).export(registry);
        Notifications.Bus.notify(
                new Notification(GROUP_DISPLAY_ID, "Decode SQLite3 Generation", "Generated successfully",
                        NotificationType.INFORMATION));
    }
}
