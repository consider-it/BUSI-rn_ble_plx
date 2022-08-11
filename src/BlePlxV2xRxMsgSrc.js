import { BleManager } from './BleManager'
import { BleMessageBuilder } from './BleMessageBuilder'
import { Subject } from 'rxjs'

export class BlePlxV2xRxMsgSrc {
  type = 2
  _manager = new BleManager()
  _subscriptions = []
  _state
  _connection
  _onConnect
  _bleMessageBuilder = new BleMessageBuilder(buffer => ({
    messageSource: 2,
    binaryEncoded: buffer
  }))
  _obuPos = new Subject()
  _itsMessages = new Subject()
  _logs = new Subject()

  constructor(onConnect) {
    this._onConnect = onConnect
    this._state = this._manager.onStateChange(this._onAdapterState, true)
  }

  connect() {
    this._manager.startDeviceScan(['9bb78000-77eb-4ed9-b7d2-48ba9bb5304d'], null, (error, device) => {
      if (error) {
        this._onConnect(error)
        this._logs.next({
          connectionState: 2,
          error
        })
      }
      if (device) {
        this._subscribeToCharas(device)
      }
    })
  }

  disconnect() {}
  getStatus() {
    return this._logs.asObservable()
  }
  getObuPos() {
    return this._obuPos.asObservable()
  }
  getItsMessages() {
    return this._itsMessages.asObservable()
  }

  _subscribeToCharas = device => {
    this._manager.stopDeviceScan()
    device
      .connect()
      .then(connectedDevice => {
        this._connection = this._manager.onDeviceDisconnected(connectedDevice.id, (_1, _2) => {
          this._connection.remove()
          this._subscriptions.forEach(sub => sub.remove())
          setTimeout(() => this.connect(this._onConnect), 3000)
        })
        if (this._connection) {
          this._subscriptions.push(this._connection)
        }
        return connectedDevice.discoverAllServicesAndCharacteristics()
      })
      .then(selectedDevice => {
        this._subscriptions = ['9bb78050-77eb-4ed9-b7d2-48ba9bb5304d'].map(chara =>
          selectedDevice.monitorCharacteristicForService(
            '9bb78000-77eb-4ed9-b7d2-48ba9bb5304d',
            chara,
            (e: BleError | null, c: Characteristic | null) => {
              if (e) {
                this._logs.next({
                  connectionState: 1,
                  error: e
                })
              }
              try {
                bleMessageBuilder.enqueuePacket(Buffer.from(c.value, 'base64'))
              } catch (error) {
                this._logs.next({
                  connectionState: 1,
                  error: error
                })
              }
            }
          )
        )
        this._onConnect()
        this._logs.next({
          connectionState: 1
        })
      })
      .catch(error => {
        this._onConnect(error)
        this._logs.next({
          connectionState: 2,
          error
        })
      })
  }

  _onAdapterState(state) {
    if (state !== 'PoweredOn') {
      this._logs.next({
        connectionState: 2,
        error: Error(`Bluetooth adapter must be powered on. Current state is ${state}`)
      })
    }
  }
}
