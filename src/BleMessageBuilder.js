import { Buffer } from 'buffer'

export class BleMessageBuilder {
  constructor(onMessageBuilt) {
    this.enqueuePacket = packet => {
      const awaiting = packet.subarray(0, 1).readUInt8()
      if (this._awaiting !== undefined && awaiting !== this._awaiting - 1) throw Error('Message skipped!')
      if (this._currentMsg) {
        this._currentMsg = Buffer.concat([this._currentMsg, packet.subarray(2)])
      } else {
        this._currentMsg = packet.subarray(2)
      }
      this._awaiting = awaiting
      if (this._awaiting === 0) {
        this.onMessageBuilt(this._currentMsg)
        this._currentMsg = undefined
        this._awaiting = undefined
      }
    }
    this.onMessageBuilt = onMessageBuilt
  }
}
