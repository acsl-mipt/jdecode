#ifndef __PHOTON_PROLOGUE__
#define __PHOTON_PROLOGUE__

#include "photon/Result.h"
#include "photon/Reader.h"
#include "photon/Writer.h"
#include "photon/Varuint.h"

typedef uint8_t PhotonGtB8;
typedef uint8_t PhotonGtU8;
typedef uint16_t PhotonGtU16;
typedef uint32_t PhotonGtU32;
typedef uint64_t PhotonGtU64;
typedef PhotonBer PhotonGtBer;

typedef struct {
   void* _stub;
} PhotonGcMain;

#endif