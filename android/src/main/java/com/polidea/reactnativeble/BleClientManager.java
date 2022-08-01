package com.polidea.reactnativeble;

import android.support.annotation.NonNull;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.polidea.multiplatformbleadapter.BleAdapter;
import com.polidea.multiplatformbleadapter.BleAdapterFactory;
import com.polidea.multiplatformbleadapter.Characteristic;
import com.polidea.multiplatformbleadapter.ConnectionOptions;
import com.polidea.multiplatformbleadapter.ConnectionState;
import com.polidea.multiplatformbleadapter.Descriptor;
import com.polidea.multiplatformbleadapter.Device;
import com.polidea.multiplatformbleadapter.OnErrorCallback;
import com.polidea.multiplatformbleadapter.OnEventCallback;
import com.polidea.multiplatformbleadapter.OnSuccessCallback;
import com.polidea.multiplatformbleadapter.RefreshGattMoment;
import com.polidea.multiplatformbleadapter.ScanResult;
import com.polidea.multiplatformbleadapter.Service;
import com.polidea.multiplatformbleadapter.errors.BleError;
import com.polidea.reactnativeble.converter.*;
import com.polidea.reactnativeble.utils.ReadableArrayConverter;
import com.polidea.reactnativeble.utils.SafePromise;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BleClientManager extends ReactContextBaseJavaModule {

    // Name of module
    private static final String NAME = "BleClientManager";

    // Value converters
    private final GenericExceptionConverter genericExceptionConverter = new GenericExceptionConverter();
    private final BleErrorToJsObjectConverter errorConverter = new BleErrorToJsObjectConverter();
    private final ScanResultToJsObjectConverter scanResultConverter = new ScanResultToJsObjectConverter();
    private final DeviceToJsObjectConverter deviceConverter = new DeviceToJsObjectConverter();
    private final CharacteristicToJsObjectConverter characteristicConverter = new CharacteristicToJsObjectConverter();
    private final DescriptorToJsObjectConverter descriptorConverter = new DescriptorToJsObjectConverter();
    private final ServiceToJsObjectConverter serviceConverter = new ServiceToJsObjectConverter();

    private BleAdapter bleAdapter;

    public BleClientManager(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        for (Event event : Event.values()) {
            constants.put(event.name, event.name);
        }
        return constants;
    }

    // Lifecycle -----------------------------------------------------------------------------------

    @ReactMethod
    public void createClient(String restoreStateIdentifier) {
        bleAdapter = BleAdapterFactory.getNewAdapter(getReactApplicationContext());
        bleAdapter.createClient(restoreStateIdentifier,
                new OnEventCallback<String>() {
                    @Override
                    public void onEvent(String state) {
                        sendEvent(Event.StateChangeEvent, state);
                    }
                }, new OnEventCallback<Integer>() {
                    @Override
                    public void onEvent(Integer data) {
                        sendEvent(Event.RestoreStateEvent, null);
                    }
                });
    }

    @ReactMethod
    public void destroyClient() {
      try {
        bleAdapter.destroyClient();
        bleAdapter = null;
      } catch (Exception e) {
        sendEvent(Event.NullAdapterEvent, e);
      }
    }

    // Mark: Common --------------------------------------------------------------------------------

    @ReactMethod
    public void cancelTransaction(String transactionId) {
      try {
        bleAdapter.cancelTransaction(transactionId);
      } catch (Exception e) {
        sendEvent(Event.NullAdapterEvent, e);
      }
    }

    @ReactMethod
    public void setLogLevel(String logLevel) {
      try {
        bleAdapter.setLogLevel(logLevel);
      } catch (Exception e) {
        sendEvent(Event.NullAdapterEvent, e);
      }
    }

    @ReactMethod
    public void logLevel(Promise promise) {
      try {
        promise.resolve(bleAdapter.getLogLevel());
      } catch (Exception e) {
        sendEvent(Event.NullAdapterEvent, e);
      }
    }

    // Mark: Monitoring state ----------------------------------------------------------------------

    @ReactMethod
    public void enable(final String transactionId, final Promise promise) {
      final SafePromise safePromise = new SafePromise(promise);
      try {
        bleAdapter.enable(transactionId, new OnSuccessCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                safePromise.resolve(null);
            }
        }, new OnErrorCallback() {
            @Override
            public void onError(BleError error) {
                safePromise.reject(null, errorConverter.toJs(error));
            }
        });
      } catch (Exception e) {
        sendEvent(Event.NullAdapterEvent, e);
      }
    }

    @ReactMethod
    public void disable(final String transactionId, final Promise promise) {
        final SafePromise safePromise = new SafePromise(promise);
        try {
          bleAdapter.disable(transactionId, new OnSuccessCallback<Void>() {
              @Override
              public void onSuccess(Void data) {
                  safePromise.resolve(null);
              }
          }, new OnErrorCallback() {
              @Override
              public void onError(BleError error) {
                  safePromise.reject(null, errorConverter.toJs(error));
              }
          });
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void state(Promise promise) {
        try {
            promise.resolve(bleAdapter.getCurrentState());
        } catch (Exception e) {
            sendEvent(Event.NullAdapterEvent, e);
        }
    }

    // Mark: Scanning ------------------------------------------------------------------------------

    @ReactMethod
    public void startDeviceScan(@Nullable ReadableArray filteredUUIDs, @Nullable ReadableMap options) {
        final int DEFAULT_SCAN_MODE_LOW_POWER = 0;
        final int DEFAULT_CALLBACK_TYPE_ALL_MATCHES = 1;

        int scanMode = DEFAULT_SCAN_MODE_LOW_POWER;
        int callbackType = DEFAULT_CALLBACK_TYPE_ALL_MATCHES;

        if (options != null) {
            if (options.hasKey("scanMode") && options.getType("scanMode") == ReadableType.Number) {
                scanMode = options.getInt("scanMode");
            }
            if (options.hasKey("callbackType") && options.getType("callbackType") == ReadableType.Number) {
                callbackType = options.getInt("callbackType");
            }
        }

        try {
          bleAdapter.startDeviceScan(
                  filteredUUIDs != null ? ReadableArrayConverter.toStringArray(filteredUUIDs) : null,
                  scanMode, callbackType,
                  new OnEventCallback<ScanResult>() {
                      @Override
                      public void onEvent(ScanResult data) {
                          sendEvent(Event.ScanEvent, scanResultConverter.toJSCallback(data));
                      }
                  }, new OnErrorCallback() {
                      @Override
                      public void onError(BleError error) {
                          sendEvent(Event.ScanEvent, errorConverter.toJSCallback(error));
                      }
                  });
          } catch (Exception e) {
            sendEvent(Event.NullAdapterEvent, e);
          }
    }

    @ReactMethod
    public void stopDeviceScan() {
        try {
            bleAdapter.stopDeviceScan();
        } catch (Exception e) {
            sendEvent(Event.NullAdapterEvent, e);
        }
    }

    // Mark: Device management ---------------------------------------------------------------------

    @ReactMethod
    public void devices(final ReadableArray deviceIdentifiers, final Promise promise) {
      try {
        bleAdapter.getKnownDevices(ReadableArrayConverter.toStringArray(deviceIdentifiers),
                new OnSuccessCallback<Device[]>() {
                    @Override
                    public void onSuccess(Device[] data) {
                        WritableArray jsDevices = Arguments.createArray();
                        for (Device device : data) {
                            jsDevices.pushMap(deviceConverter.toJSObject(device));
                        }
                        promise.resolve(jsDevices);
                    }
                }, new OnErrorCallback() {
                    @Override
                    public void onError(BleError error) {
                        promise.reject(null, errorConverter.toJs(error));
                    }
                });
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void connectedDevices(final ReadableArray serviceUUIDs, final Promise promise) {
      try {
        bleAdapter.getConnectedDevices(ReadableArrayConverter.toStringArray(serviceUUIDs),
                new OnSuccessCallback<Device[]>() {
                    @Override
                    public void onSuccess(Device[] data) {
                        final WritableArray writableArray = Arguments.createArray();
                        for (Device device : data) {
                            writableArray.pushMap(deviceConverter.toJSObject(device));
                        }
                        promise.resolve(writableArray);
                    }
                }, new OnErrorCallback() {
                    @Override
                    public void onError(BleError error) {
                        promise.reject(null, errorConverter.toJs(error));
                    }
                });
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    // Mark: Device operations ---------------------------------------------------------------------

    @ReactMethod
    public void requestConnectionPriorityForDevice(final String deviceId, int connectionPriority, final String transactionId, final Promise promise) {
        final SafePromise safePromise = new SafePromise(promise);
        try {
          bleAdapter.requestConnectionPriorityForDevice(deviceId, connectionPriority, transactionId,
                  new OnSuccessCallback<Device>() {
                      @Override
                      public void onSuccess(Device data) {
                          safePromise.resolve(deviceConverter.toJSObject(data));
                      }
                  }, new OnErrorCallback() {
                      @Override
                      public void onError(BleError error) {
                          safePromise.reject(null, errorConverter.toJs(error));
                      }
                  });
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void requestMTUForDevice(final String deviceId, int mtu, final String transactionId, final Promise promise) {
        final SafePromise safePromise = new SafePromise(promise);
        try {
          bleAdapter.requestMTUForDevice(deviceId, mtu, transactionId,
                  new OnSuccessCallback<Device>() {
                      @Override
                      public void onSuccess(Device data) {
                          safePromise.resolve(deviceConverter.toJSObject(data));
                      }
                  }, new OnErrorCallback() {
                      @Override
                      public void onError(BleError error) {
                          safePromise.reject(null, errorConverter.toJs(error));
                      }
                  });
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void readRSSIForDevice(final String deviceId, final String transactionId, final Promise promise) {
        final SafePromise safePromise = new SafePromise(promise);
        try {
          bleAdapter.readRSSIForDevice(deviceId, transactionId,
                new OnSuccessCallback<Device>() {
                    @Override
                    public void onSuccess(Device data) {
                        safePromise.resolve(deviceConverter.toJSObject(data));
                    }
                }, new OnErrorCallback() {
                    @Override
                    public void onError(BleError error) {
                        safePromise.reject(null, errorConverter.toJs(error));
                    }
                });
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void connectToDevice(final String deviceId, @Nullable ReadableMap options, final Promise promise) {
        final SafePromise safePromise = new SafePromise(promise);

        boolean autoConnect = false;
        int requestMtu = 0;
        RefreshGattMoment refreshGattMoment = null;
        Integer timeout = null;
        int connectionPriority = 0; // CONNECTION_PRIORITY_BALANCED

        if (options != null) {
            if (options.hasKey("autoConnect") && options.getType("autoConnect") == ReadableType.Boolean) {
                autoConnect = options.getBoolean("autoConnect");
            }
            if (options.hasKey("requestMTU") && options.getType("requestMTU") == ReadableType.Number) {
                requestMtu = options.getInt("requestMTU");
            }
            if (options.hasKey("refreshGatt") && options.getType("refreshGatt") == ReadableType.String) {
                refreshGattMoment = RefreshGattMoment.getByName(options.getString("refreshGatt"));
            }
            if (options.hasKey("timeout") && options.getType("timeout") == ReadableType.Number) {
                timeout = options.getInt("timeout");
            }
            if (options.hasKey("connectionPriority") && options.getType("connectionPriority") == ReadableType.Number) {
                connectionPriority = options.getInt("connectionPriority");
            }
        }
        try {
          bleAdapter.connectToDevice(
                  deviceId,
                  new ConnectionOptions(autoConnect,
                          requestMtu,
                          refreshGattMoment,
                          timeout != null ? timeout.longValue() : null,
                          connectionPriority),
                  new OnSuccessCallback<Device>() {
                      @Override
                      public void onSuccess(Device data) {
                          safePromise.resolve(deviceConverter.toJSObject(data));
                      }
                  },
                  new OnEventCallback<ConnectionState>() {
                      @Override
                      public void onEvent(ConnectionState connectionState) {
                          if (connectionState == ConnectionState.DISCONNECTED) {
                              WritableArray event = Arguments.createArray();
                              event.pushNull();
                              WritableMap device = Arguments.createMap();
                              device.putString("id", deviceId);
                              event.pushMap(device);
                              sendEvent(Event.DisconnectionEvent, event);
                          }
                      }
                  },
                  new OnErrorCallback() {
                      @Override
                      public void onError(BleError error) {
                          safePromise.reject(null, errorConverter.toJs(error));
                      }
                  });
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void cancelDeviceConnection(String deviceId, Promise promise) {
        final SafePromise safePromise = new SafePromise(promise);
        try {
        bleAdapter.cancelDeviceConnection(deviceId,
                new OnSuccessCallback<Device>() {
                    @Override
                    public void onSuccess(Device data) {
                        safePromise.resolve(deviceConverter.toJSObject(data));
                    }
                }, new OnErrorCallback() {
                    @Override
                    public void onError(BleError error) {
                        safePromise.reject(null, errorConverter.toJs(error));
                    }
                });
                } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void isDeviceConnected(String deviceId, final Promise promise) {
      try {
        bleAdapter.isDeviceConnected(deviceId,
                new OnSuccessCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean isConnected) {
                        promise.resolve(isConnected);
                    }
                }, new OnErrorCallback() {
                    @Override
                    public void onError(BleError error) {
                        promise.reject(null, errorConverter.toJs(error));
                    }
                });
                } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    // Mark: Discovery -----------------------------------------------------------------------------

    @ReactMethod
    public void discoverAllServicesAndCharacteristicsForDevice(String deviceId, final String transactionId, final Promise promise) {
        final SafePromise safePromise = new SafePromise(promise);
        try {
        bleAdapter.discoverAllServicesAndCharacteristicsForDevice(deviceId, transactionId,
                new OnSuccessCallback<Device>() {
                    @Override
                    public void onSuccess(Device data) {
                        safePromise.resolve(deviceConverter.toJSObject(data));
                    }
                }, new OnErrorCallback() {
                    @Override
                    public void onError(BleError error) {
                        safePromise.reject(null, errorConverter.toJs(error));
                    }
                });
                } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    // Mark: Service and characteristic getters ----------------------------------------------------

    @ReactMethod
    public void servicesForDevice(final String deviceId, final Promise promise) {
        try {
            List<Service> services = bleAdapter.getServicesForDevice(deviceId);
            WritableArray jsArray = Arguments.createArray();
            for (Service service : services) {
                jsArray.pushMap(serviceConverter.toJSObject(service));
            }
            promise.resolve(jsArray);
        } catch (BleError error) {
            promise.reject(null, errorConverter.toJs(error));
        }

    }

    @ReactMethod
    public void characteristicsForDevice(final String deviceId,
                                         final String serviceUUID,
                                         final Promise promise) {
        try {
            List<Characteristic> characteristics = bleAdapter.getCharacteristicsForDevice(deviceId, serviceUUID);

            WritableArray jsCharacteristics = Arguments.createArray();
            for (Characteristic characteristic : characteristics) {
                jsCharacteristics.pushMap(characteristicConverter.toJSObject(characteristic));
            }
            promise.resolve(jsCharacteristics);
        } catch (BleError error) {
            promise.reject(null, errorConverter.toJs(error));
        }
    }

    @ReactMethod
    public void characteristicsForService(final int serviceIdentifier, final Promise promise) {
        try {
            List<Characteristic> characteristics = bleAdapter.getCharacteristicsForService(serviceIdentifier);
            WritableArray jsCharacteristics = Arguments.createArray();
            for (Characteristic characteristic : characteristics) {
                jsCharacteristics.pushMap(characteristicConverter.toJSObject(characteristic));
            }
            promise.resolve(jsCharacteristics);
        } catch (BleError error) {
            promise.reject(null, errorConverter.toJs(error));
        }
    }

    @ReactMethod
    public void descriptorsForDevice(final String deviceIdentifier,
                                     final String serviceUUID,
                                     final String characteristicUUID,
                                     final Promise promise) {
        try {
            List<Descriptor> descriptors = bleAdapter.descriptorsForDevice(deviceIdentifier, serviceUUID, characteristicUUID);
            WritableArray jsDescriptors = Arguments.createArray();
            for (Descriptor descriptor : descriptors) {
                jsDescriptors.pushMap(descriptorConverter.toJSObject(descriptor));
            }
            promise.resolve(jsDescriptors);
        } catch (BleError error) {
            promise.reject(null, errorConverter.toJs(error));
        }
    }

    @ReactMethod
    public void descriptorsForService(final int serviceIdentifier,
                                      final String characteristicUUID,
                                      final Promise promise) {
        try {
            List<Descriptor> descriptors = bleAdapter.descriptorsForService(serviceIdentifier, characteristicUUID);
            WritableArray jsDescriptors = Arguments.createArray();
            for (Descriptor descriptor : descriptors) {
                jsDescriptors.pushMap(descriptorConverter.toJSObject(descriptor));
            }
            promise.resolve(jsDescriptors);
        } catch (BleError error) {
            promise.reject(null, errorConverter.toJs(error));
        }
    }

    @ReactMethod
    public void descriptorsForCharacteristic(final int characteristicIdentifier,
                                             final Promise promise) {
        try {
            List<Descriptor> descriptors = bleAdapter.descriptorsForCharacteristic(characteristicIdentifier);
            WritableArray jsDescriptors = Arguments.createArray();
            for (Descriptor descriptor : descriptors) {
                jsDescriptors.pushMap(descriptorConverter.toJSObject(descriptor));
            }
            promise.resolve(jsDescriptors);
        } catch (BleError error) {
            promise.reject(null, errorConverter.toJs(error));
        }
    }

    // Mark: Characteristics operations ------------------------------------------------------------

    @ReactMethod
    public void writeCharacteristicForDevice(final String deviceId,
                                             final String serviceUUID,
                                             final String characteristicUUID,
                                             final String valueBase64,
                                             final Boolean response,
                                             final String transactionId,
                                             final Promise promise) {
        final SafePromise safePromise = new SafePromise(promise);

        try {
        bleAdapter.writeCharacteristicForDevice(
                deviceId, serviceUUID, characteristicUUID, valueBase64, response, transactionId,
                new OnSuccessCallback<Characteristic>() {
                    @Override
                    public void onSuccess(Characteristic data) {
                        safePromise.resolve(characteristicConverter.toJSObject(data));
                    }
                }, new OnErrorCallback() {
                    @Override
                    public void onError(BleError error) {
                        safePromise.reject(null, errorConverter.toJs(error));
                    }
                }
        );
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void writeCharacteristicForService(final int serviceIdentifier,
                                              final String characteristicUUID,
                                              final String valueBase64,
                                              final Boolean response,
                                              final String transactionId,
                                              final Promise promise) {
        final SafePromise safePromise = new SafePromise(promise);
        try {
        bleAdapter.writeCharacteristicForService(
                serviceIdentifier, characteristicUUID, valueBase64, response, transactionId,
                new OnSuccessCallback<Characteristic>() {
                    @Override
                    public void onSuccess(Characteristic data) {
                        safePromise.resolve(characteristicConverter.toJSObject(data));
                    }
                }, new OnErrorCallback() {
                    @Override
                    public void onError(BleError error) {
                        safePromise.reject(null, errorConverter.toJs(error));
                    }
                }
        );
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void writeCharacteristic(final int characteristicIdentifier,
                                    final String valueBase64,
                                    final Boolean response,
                                    final String transactionId,
                                    final Promise promise) {
        final SafePromise safePromise = new SafePromise(promise);

        try {
        bleAdapter.writeCharacteristic(characteristicIdentifier, valueBase64, response, transactionId,
                new OnSuccessCallback<Characteristic>() {
                    @Override
                    public void onSuccess(Characteristic data) {
                        safePromise.resolve(characteristicConverter.toJSObject(data));
                    }
                }, new OnErrorCallback() {
                    @Override
                    public void onError(BleError error) {
                        safePromise.reject(null, errorConverter.toJs(error));
                    }
                });
                } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void readCharacteristicForDevice(final String deviceId,
                                            final String serviceUUID,
                                            final String characteristicUUID,
                                            final String transactionId,
                                            final Promise promise) {
        final SafePromise safePromise = new SafePromise(promise);

        try {
        bleAdapter.readCharacteristicForDevice(
                deviceId, serviceUUID, characteristicUUID, transactionId,
                new OnSuccessCallback<Characteristic>() {
                    @Override
                    public void onSuccess(Characteristic data) {
                        safePromise.resolve(characteristicConverter.toJSObject(data));
                    }
                }, new OnErrorCallback() {
                    @Override
                    public void onError(BleError error) {
                        safePromise.reject(null, errorConverter.toJs(error));
                    }
                }
        );
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void readCharacteristicForService(final int serviceIdentifier,
                                             final String characteristicUUID,
                                             final String transactionId,
                                             final Promise promise) {
        final SafePromise safePromise = new SafePromise(promise);

        try {
        bleAdapter.readCharacteristicForService(
                serviceIdentifier, characteristicUUID, transactionId,
                new OnSuccessCallback<Characteristic>() {
                    @Override
                    public void onSuccess(Characteristic data) {
                        safePromise.resolve(characteristicConverter.toJSObject(data));
                    }
                }, new OnErrorCallback() {
                    @Override
                    public void onError(BleError error) {
                        safePromise.reject(null, errorConverter.toJs(error));
                    }
                }
        );
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void readCharacteristic(final int characteristicIdentifier,
                                   final String transactionId,
                                   final Promise promise) {
        final SafePromise safePromise = new SafePromise(promise);

        try {
        bleAdapter.readCharacteristic(
                characteristicIdentifier, transactionId,
                new OnSuccessCallback<Characteristic>() {
                    @Override
                    public void onSuccess(Characteristic data) {
                        safePromise.resolve(characteristicConverter.toJSObject(data));
                    }
                }, new OnErrorCallback() {
                    @Override
                    public void onError(BleError error) {
                        safePromise.reject(null, errorConverter.toJs(error));
                    }
                }
        );
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void monitorCharacteristicForDevice(final String deviceId,
                                               final String serviceUUID,
                                               final String characteristicUUID,
                                               final String transactionId,
                                               final Promise promise) {
        final SafePromise safePromise = new SafePromise(promise);
        try {
        bleAdapter.monitorCharacteristicForDevice(
                deviceId, serviceUUID, characteristicUUID, transactionId,
                new OnEventCallback<Characteristic>() {
                    @Override
                    public void onEvent(Characteristic data) {
                        WritableArray jsResult = Arguments.createArray();
                        jsResult.pushNull();
                        jsResult.pushMap(characteristicConverter.toJSObject(data));
                        jsResult.pushString(transactionId);
                        sendEvent(Event.ReadEvent, jsResult);
                    }
                }, new OnErrorCallback() {
                    @Override
                    public void onError(BleError error) {
                        safePromise.reject(null, errorConverter.toJs(error));
                    }
                }
        );
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void monitorCharacteristicForService(final int serviceIdentifier,
                                                final String characteristicUUID,
                                                final String transactionId,
                                                final Promise promise) {
        final SafePromise safePromise = new SafePromise(promise);
        try {
        bleAdapter.monitorCharacteristicForService(
                serviceIdentifier, characteristicUUID, transactionId,
                new OnEventCallback<Characteristic>() {
                    @Override
                    public void onEvent(Characteristic data) {
                        WritableArray jsResult = Arguments.createArray();
                        jsResult.pushNull();
                        jsResult.pushMap(characteristicConverter.toJSObject(data));
                        jsResult.pushString(transactionId);
                        sendEvent(Event.ReadEvent, jsResult);
                    }
                }, new OnErrorCallback() {
                    @Override
                    public void onError(BleError error) {
                        safePromise.reject(null, errorConverter.toJs(error));
                    }
                }
        );
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void monitorCharacteristic(final int characteristicIdentifier,
                                      final String transactionId,
                                      final Promise promise) {
        final SafePromise safePromise = new SafePromise(promise);
        //TODO resolve safePromise with null when monitoring has been completed
        try {
        bleAdapter.monitorCharacteristic(
                characteristicIdentifier, transactionId,
                new OnEventCallback<Characteristic>() {
                    @Override
                    public void onEvent(Characteristic data) {
                        WritableArray jsResult = Arguments.createArray();
                        jsResult.pushNull();
                        jsResult.pushMap(characteristicConverter.toJSObject(data));
                        jsResult.pushString(transactionId);
                        sendEvent(Event.ReadEvent, jsResult);
                    }
                }, new OnErrorCallback() {
                    @Override
                    public void onError(BleError error) {
                        safePromise.reject(null, errorConverter.toJs(error));
                    }
                }
        );
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void readDescriptorForDevice(final String deviceId,
                                        final String serviceUUID,
                                        final String characteristicUUID,
                                        final String descriptorUUID,
                                        final String transactionId,
                                        final Promise promise) {
                                          try {
        bleAdapter.readDescriptorForDevice(
                deviceId,
                serviceUUID,
                characteristicUUID,
                descriptorUUID,
                transactionId,
                new OnSuccessCallback<Descriptor>() {
                    @Override
                    public void onSuccess(Descriptor descriptor) {
                        promise.resolve(descriptorConverter.toJSObject(descriptor));
                    }
                }, new OnErrorCallback() {
                    @Override
                    public void onError(BleError bleError) {
                        promise.reject(null, errorConverter.toJs(bleError));
                    }
                });
                } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void readDescriptorForService(final int serviceIdentifier,
                                         final String characteristicUUID,
                                         final String descriptorUUID,
                                         final String transactionId,
                                         final Promise promise) {
                                          try {
        bleAdapter.readDescriptorForService(
                serviceIdentifier,
                characteristicUUID,
                descriptorUUID,
                transactionId,
                new OnSuccessCallback<Descriptor>() {
                    @Override
                    public void onSuccess(Descriptor descriptor) {
                        promise.resolve(descriptorConverter.toJSObject(descriptor));
                    }
                },
                new OnErrorCallback() {
                    @Override
                    public void onError(BleError bleError) {
                        promise.reject(null, errorConverter.toJs(bleError));
                    }
                });
                } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void readDescriptorForCharacteristic(final int characteristicIdentifier,
                                                final String descriptorUUID,
                                                final String transactionId,
                                                final Promise promise) {
                                                  try {
        bleAdapter.readDescriptorForCharacteristic(
                characteristicIdentifier,
                descriptorUUID,
                transactionId,
                new OnSuccessCallback<Descriptor>() {
                    @Override
                    public void onSuccess(Descriptor descriptor) {
                        promise.resolve(descriptorConverter.toJSObject(descriptor));
                    }
                },
                new OnErrorCallback() {
                    @Override
                    public void onError(BleError bleError) {
                        promise.reject(null, errorConverter.toJs(bleError));
                    }
                });
                } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void readDescriptor(final int descriptorIdentifier,
                               final String transactionId,
                               final Promise promise) {
                                try {
        bleAdapter.readDescriptor(
                descriptorIdentifier,
                transactionId,
                new OnSuccessCallback<Descriptor>() {
                    @Override
                    public void onSuccess(Descriptor descriptor) {
                        promise.resolve(descriptorConverter.toJSObject(descriptor));
                    }
                },
                new OnErrorCallback() {
                    @Override
                    public void onError(BleError bleError) {
                        promise.reject(null, errorConverter.toJs(bleError));
                    }
                });
                } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void writeDescriptorForDevice(final String deviceId,
                                         final String serviceUUID,
                                         final String characteristicUUID,
                                         final String descriptorUUID,
                                         final String valueBase64,
                                         final String transactionId,
                                         final Promise promise) {
                                          try {
        bleAdapter.writeDescriptorForDevice(
                deviceId,
                serviceUUID,
                characteristicUUID,
                descriptorUUID,
                valueBase64,
                transactionId,
                new OnSuccessCallback<Descriptor>() {
                    @Override
                    public void onSuccess(Descriptor descriptor) {
                        promise.resolve(descriptorConverter.toJSObject(descriptor));
                    }
                },
                new OnErrorCallback() {
                    @Override
                    public void onError(BleError bleError) {
                        promise.reject(null, errorConverter.toJs(bleError));
                    }
                }
        );
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void writeDescriptorForService(final int serviceIdentifier,
                                          final String characteristicUUID,
                                          final String descriptorUUID,
                                          final String valueBase64,
                                          final String transactionId,
                                          final Promise promise) {
                                            try {
        bleAdapter.writeDescriptorForService(
                serviceIdentifier,
                characteristicUUID,
                descriptorUUID,
                valueBase64,
                transactionId,
                new OnSuccessCallback<Descriptor>() {
                    @Override
                    public void onSuccess(Descriptor descriptor) {
                        promise.resolve(descriptorConverter.toJSObject(descriptor));
                    }
                },
                new OnErrorCallback() {
                    @Override
                    public void onError(BleError bleError) {
                        promise.reject(null, errorConverter.toJs(bleError));
                    }
                }
        );
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void writeDescriptorForCharacteristic(final int characteristicIdentifier,
                                                 final String descriptorUUID,
                                                 final String valueBase64,
                                                 final String transactionId,
                                                 final Promise promise) {
                                                  try {
        bleAdapter.writeDescriptorForCharacteristic(
                characteristicIdentifier,
                descriptorUUID,
                valueBase64,
                transactionId,
                new OnSuccessCallback<Descriptor>() {
                    @Override
                    public void onSuccess(Descriptor descriptor) {
                        promise.resolve(descriptorConverter.toJSObject(descriptor));
                    }
                },
                new OnErrorCallback() {
                    @Override
                    public void onError(BleError bleError) {
                        promise.reject(null, errorConverter.toJs(bleError));
                    }
                }
        );
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void writeDescriptor(final int descriptorIdentifier,
                                final String valueBase64,
                                final String transactionId,
                                final Promise promise) {
                                  try {
        bleAdapter.writeDescriptor(
                descriptorIdentifier,
                valueBase64,
                transactionId,
                new OnSuccessCallback<Descriptor>() {
                    @Override
                    public void onSuccess(Descriptor descriptor) {
                        promise.resolve(descriptorConverter.toJSObject(descriptor));
                    }
                },
                new OnErrorCallback() {
                    @Override
                    public void onError(BleError bleError) {
                        promise.reject(null, errorConverter.toJs(bleError));
                    }
                }
        );
        } catch (Exception e) {
          sendEvent(Event.NullAdapterEvent, e);
        }
    }

    @ReactMethod
    public void addListener(String eventName) {
      // Keep: Required for RN built in Event Emitter Calls.  
    }

    @ReactMethod
    public void removeListeners(int count) {
      // Keep: Required for RN built in Event Emitter Calls.
    }

    private void sendEvent(@NonNull Event event, @Nullable Object params) {
        try {
            getReactApplicationContext()
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(event.name, params);
        } catch (Exception e) {
            sendEvent(Event.NullAdapterEvent, genericExceptionConverter.toJs(e));
        }
    }
}
