package ru.mipt.acsl.decode.model.domain
package component
package message

import ru.mipt.acsl.decode.model.domain.impl.types.AbstractHasInfo

/**
  * @author Artem Shein
  */
private[domain] abstract class AbstractMessage(info: LocalizedString) extends AbstractHasInfo(info) with TmMessage
