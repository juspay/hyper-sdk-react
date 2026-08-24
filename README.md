# Hyper-SDK-React

React native module for HyperSDK which enables payment orchestration via different dynamic modules. More details available at Juspay Developer Docs for [Express Checkout SDK](https://developer.juspay.in/v2.0/docs/introduction) and [Payment Page SDK](https://developer.juspay.in/v4.0/docs/introduction). Some part of module depends heavily on native functionalities are not updatable dynamically.

## Installation

```sh
npm install hyper-sdk-react
```

### Android

Add following maven url in the allProjects > repositories section of root(top) build.gradle:

```groovy
maven { url "https://maven.juspay.in/jp-build-packages/hyper-sdk/" }
```

Add the clientId ext property in root(top) `build.gradle`:

```groovy
buildscript {
    ....
    ext {
        ....
        clientId = "<clientId shared by Juspay team>"
        hyperSDKVersion = "2.2.1"
        ....
    }
    ....
}
```
- You can also provide an override for base SDK version present in plugin (the newer version among both would be considered). - Optional
- Exclude microSDKs provided with HyperSDK for given clientId by adding excludedMicroSDKs - Optional

### iOS

Run the following command inside the ios folder of your react native project:

```sh
pod install
```

**(Optional)** Add the following property in `package.json` of your project before running pod install if you want to override the base SDK version present in the plugin (the newer version among both would be considered):

```json
  {
    ....
    "scripts": {
      ....
    },
    "dependencies": {
      ....
    },
    "devDependencies": {
      ....
    },
    "hyperSdkIOSVersion": "2.2.2"
    ....
  }
```

Note: This version is just for explanatory purposes and may change in future. Contact Juspay support team for the latest SDK version.

#### **Dynamic Assets iOS**

Change the `hyperSdkIOSVersion` to `2.2.2` (This version is just for explanatory purposes and may change in future. Contact Juspay support team for the latest SDK version).

Add below post_install script in the Podfile

```sh
post_install do |installer|
 fuse_path = "./Pods/HyperSDK/Fuse.rb"
 clean_assets = false # Pass true to re-download all the assets
 if File.exist?(fuse_path)
   if system("ruby", fuse_path.to_s, clean_assets.to_s)
   end
 end
end
```

Place the `MerchantConfig.txt` file inside the folder where the Podfile is present. This file doesn't need to be added to the project. The content of the file should be as below

```txt
clientId = <clientId shared by Juspay Team>
```

## Usage

### Exposed APIs

```ts
type HyperSdkReactType = {
  HyperEvent: string;
  preFetch(data: string): void;
  createHyperServices(): void;
  createHyperServicesWithTenantId(tenantId: string, clientId: string): void;
  initiate(data: string): void;
  process(data: string): void;
  processWithActivity(data: string): void;
  terminate(): void;
  onBackPressed(): boolean;
  isNull(): boolean;
  isInitialised(): Promise<boolean>;
  updateBaseViewController(): void;
};

const { HyperSdkReact } = NativeModules;

export default HyperSdkReact as HyperSdkReactType;
```

For apps that need more than one tenant / client in the same session, the module also exports a
`HyperServiceInstance` class. See [Multiple HyperServices Instances](#multiple-hyperservices-instances).

```ts
import HyperSdkReact, { HyperServiceInstance } from 'hyper-sdk-react';
```

### Import HyperSDK

```ts
import HyperSdkReact from 'hyper-sdk-react';
```

### Step-0: PreFetch

To keep the SDK up to date with the latest changes, it is highly recommended to call `preFetch` as early as possible. It takes a `stringified JSON` as its argument.

```ts
HyperSdkReact.preFetch(JSON.stringify(preFetchPayload));
```

### Step-1: Create HyperServices Object

This method creates an instance of `HyperServices` class in the React Bridge Module on which all the `HyperSDK` APIs / methods are triggered. It internally uses the current activity as an argument.

**Note**: This method is mandatory and is required to call any other subsequent methods from `HyperSDK`.

```ts
HyperSdkReact.createHyperServices();
```

This creates a single, module-wide instance. If you need several independent instances (multiple
tenants / clients), use [`HyperServiceInstance`](#multiple-hyperservices-instances) instead.

### Step-2: Initiate

This method should be called on the render of the host screen. This will boot up the SDK and start the Hyper engine. It takes a `stringified JSON` as its argument which will contain the base parameters for the entire session and remains static throughout one SDK instance lifetime.

Initiate is an asynchronous call and its result (whether success or failure) is provided in the `Hyper Event listener`, later discussed in [step-4](#step-4-listen-to-events-from-hypersdk).

**Note**: It is highly recommended to initiate SDK from the order summary page (at least 5 seconds before opening your payment page) for seamless user experience.

```ts
HyperSdkReact.initiate(JSON.stringify(initiatePayload));
```

### Step-3: Process

This API should be triggered for all operations required from `HyperSDK`. The operation may be related to:

- Displaying payment options on your payment page
- Performing a transaction
- User's payment profile management

The result of the process call is provided in the `Hyper Event listener`, later discussed in [step-4](#step-4-listen-to-events-from-hypersdk).

```ts
HyperSdkReact.process(JSON.stringify(processPayload));
```

If any of the react-native library is impacting the UI/UX, please use `processWithActivity` instead, which starts a new Activity for opening the Payment Page, isolated of react native.

```ts
HyperSdkReact.processWithActivity(JSON.stringify(processPayload));
```

### Step-4: Listen to events from HyperSDK

`Hyper SDK` Native Module will be emitting all the relevant events to JS via `RCTDeviceEventEmitter` and JavaScript modules can then register to receive events by invoking `addListener` on the `NativeEventEmitter` class in the `componentDidMount()` method with the event name `'HyperEvent'` (You can use the `HyperSdkReact.HyperEvent` as well). The listener will return a `stringified JSON` response (`resp`).

The following events should be handled here:

- `show_loader`: To show a loader for the processing state.
- `hide_loader`: To hide the previously shown loader.
- `initiate_result`: Result of initiate done in [step-2](#step-2-initiate).
- `process_result`: Result of the process operation done in [step-3](#step-3-process).

**Note**: The listener can be removed when the React component unmounts in `componentWillUnmount()` method.

```ts
 componentDidMount() {
   ...
   const eventEmitter = new NativeEventEmitter(NativeModules.HyperSdkReact);
   this.eventListener = eventEmitter.addListener(HyperSdkReact.HyperEvent, (resp) => {
     var data = JSON.parse(resp);
     var event: string = data.event || '';
     switch (event) {
       case 'show_loader':
         // show some loader here
         break;

       case 'hide_loader':
         // hide the loader
         break;

       case 'initiate_result':
         var payload = data.payload || {};
         console.log('initiate_result: ', payload);
         // merchant code
         ...
         break;

       case 'process_result':
         var payload = data.payload || {};
         console.log('process_result: ', payload);
         // merchant code
         ...
         break;

       default:
         console.log('Unknown Event', data);
     }
     ...
   });
   ...
 }

 componentWillUnmount() {
   ...
   this.eventListener.remove();
   ...
 }
```

### Step-5: Android Hardware Back-Press Handling

`Hyper SDK` internally uses an android fragment for opening the bank page and will need the control to hardware back press when the bank page is active. This can be done by invoking `addEventListener` on the `BackHandler` provided by React-Native.

If the blocking asynchronous call `HyperSdkReact.onBackPressed()` returns true, `Hyper SDK` will handle the back press, else merchant can handle it.

**Note**: `HyperSdkReact.isNull()` (refer [here](#helper-is-null)) can also be called before calling `onBackPressed()` to ensure that the HyperServices object is not null.

```ts
 componentDidMount() {
   ...
   BackHandler.addEventListener('hardwareBackPress', () => {
     return !HyperSdkReact.isNull() && HyperSdkReact.onBackPressed();
   });
   ...
 }

 componentWillUnmount() {
   ...
   BackHandler.removeEventListener('hardwareBackPress', () => null);
   ...
 }
```

### Step-6: Android Permissions Handling

Hyper SDK needs to listen to the response of permissions asked to the user for handling auto SMS reading (wherever applicable). To do so, the merchant's activity should delegate the response to Hyper SDK once it is received from the user. This can be done by adding the following snippet in merchant's react activity (`MainActivity`):

```java
  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
      if (HyperSdkReactModule.getPermissionRequestCodes().contains(requestCode)) {
          HyperSdkReactModule.onRequestPermissionsResult(requestCode, permissions, grantResults);
      } else {
          super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      }
  }
```

### Step-7: Terminate

This method shall be triggered when `HyperSDK` is no longer required.

```ts
HyperSdkReact.terminate();
```

### Helper: Is Null

This is a helper method and can be used to check whether the `HyperServices` object is `null` at any particular moment. It is a blocking synchronous method and returns a `boolean` value.

```ts
var isNull: boolean = HyperSdkReact.isNull();
console.log('is HyperSDK null: ', isNull);
```

### Optional: Is Initialised

This is a helper / optional method to check whether SDK has been initialised after [step-2](#step-2-initiate). It returns a `JS Promise` with a `boolean` value.

```ts
HyperSdkReact.isInitialised().then((init: boolean) => {
  console.log('isInitialised:', init);
});
```

### Optional: Update Base ViewController - Only for iOS

This is an optional method to update the base view controller in case if any new view controller is presented over top view controller after the SDK initiation. This method should be called before making `HyperSdkReact.process()` call.

```ts
HyperSdkReact.updateBaseViewController();
```

### Optional: Support for adding merchant views

This sections helps to attach custom views inside designated sections in the payment page. You will need to register the component to be attached under one of the following names, based on where the component is attached.

1. JuspayHeaderAttached
1. JuspayHeader
1. JuspayFooter
1. JuspayFooterAttached

You can follow the below syntax to attach the component.
```ts
HyperSdkReact.notifyAboutRegisterComponent(HyperSdkReact.JuspayHeaderAttached)
AppRegistry.registerComponent(HyperSdkReact.JuspayHeaderAttached, () => CustomComponent);
```

Please note component must be registered before calling process call of the sdk.

Note: In iOS we are not able to infer the height of the component being rendered.
Therefore the component must fire `HyperSdkReact.updateMerchantViewHeight(<section_name>, <height>);`

For example
```ts
HyperSdkReact.updateMerchantViewHeight(HyperSdkReact.JuspayHeader, 200);
```

If your view dynamically computes height. Height can be obtained by adding the following property to the root of component registered

#### For react-native old architecture
```ts
  onLayout={(event) => {
        const { height } = event.nativeEvent.layout;
        HyperSdkReact.updateMerchantViewHeight(HyperSdkReact.JuspayHeader, height);
      }}

```

#### For react-native new architecture
```ts
  const ref = useRef<View>(null);
  useLayoutEffect(() => {
    if (ref.current?.measure) {
      ref.current.measure((x, y, width, height, pageX, pageY) => {
        HyperSdkReact.updateMerchantViewHeight(HyperSdkReact.JuspayHeader, height);
      });
    }
  }, []);

  <View ref={ref}> </View>

```

#### If your AppDelegate is in swift
If your `AppDelegate` is in `swift` and you are using react native version greater than or equal to 0.78, then your AppDelegate.swift has to implement the protocol `HyperSdkReactDelegate` and return the `reactNativeFactory` property from the `getReactNativeFactory` function. This helps us to render your view(Merchant View) in our payment page.

```swift
  ...
  import hyper_sdk_react

  @main
  class AppDelegate: UIResponder, UIApplicationDelegate, HyperSdkReactDelegate {
    ...
    var reactNativeFactory: RCTReactNativeFactory?
    ...
    
    func getReactNativeFactory() -> Any! {
      return reactNativeFactory
    }
  }

```

## Multiple HyperServices Instances

By default the module keeps a **single** `HyperServices` object, and every top-level API
(`HyperSdkReact.initiate`, `HyperSdkReact.process`, …) operates on it. If your app needs to talk to
more than one tenant / client in the same session — for example a marketplace that switches between
two Juspay merchants, or a super-app hosting several sub-brands — you can create independent
instances with `HyperServiceInstance`.

Each instance owns its own `HyperServices` object natively and **emits its events on its own channel**.
This is the only real difference in the integration: the event name is no longer the constant
`HyperSdkReact.HyperEvent`, it is the instance's own key, returned by `getHyperEventString()`.

### Step-1: Import

```ts
import HyperSdkReact, { HyperServiceInstance } from 'hyper-sdk-react';
```

### Step-2: Create an instance

Replaces `HyperSdkReact.createHyperServices()` / `HyperSdkReact.createHyperServicesWithTenantId()`.
The constructor allocates the native object immediately and generates the instance key.

```ts
// Default tenant / client
const instance = new HyperServiceInstance();

// Explicit tenant and client
const tenantInstance = new HyperServiceInstance(tenantId, clientId);
```

On Android the native object can only be created while an activity is in the foreground, and creation
happens asynchronously on the native side — so a synchronous `isNull()` right after the constructor
will still report `true`. If you want to confirm creation succeeded, check `isNull()` on a later tick
(for example just before calling `initiate`). Hold on to the object for the whole
lifetime of the flow — an instance can only be addressed through the reference you keep in JS. A common pattern is a `Map` keyed by `getHyperEventString()`:

```ts
const instances = new Map<string, HyperServiceInstance>();
const instance = new HyperServiceInstance(tenantId, clientId);
instances.set(instance.getHyperEventString(), instance);
```

`preFetch` stays global and is still called once as `HyperSdkReact.preFetch(...)` — it is not per-instance.

### Step-3: Listen to events from this instance

Register the listener **before** calling `initiate`, using the instance's key as the event name.
Events for one instance are never delivered on another instance's channel, nor on `HyperSdkReact.HyperEvent`.

```ts
componentDidMount() {
  const eventEmitter = new NativeEventEmitter(NativeModules.HyperSdkReact);

  this.eventListener = eventEmitter.addListener(
    this.instance.getHyperEventString(),
    (resp) => {
      const data = JSON.parse(resp);
      switch (data.event || '') {
        case 'show_loader':
          break;
        case 'hide_loader':
          break;
        case 'initiate_result':
          console.log('initiate_result: ', data.payload || {});
          break;
        case 'process_result':
          console.log('process_result: ', data.payload || {});
          break;
        default:
          console.log('Unknown Event', data);
      }
    }
  );
}

componentWillUnmount() {
  this.eventListener.remove();
}
```

**Note**: the key is generated per instance, so it must be threaded through to whichever screen
listens for the response. If you navigate to another screen to run `process`, pass the instance (or
its key) through the navigation params and subscribe there.

### Step-4: Initiate and Process

Same payloads as the single-instance API — only the receiver changes.

```ts
instance.initiate(JSON.stringify(initiatePayload));
instance.process(JSON.stringify(processPayload));
```

### Step-5: Android hardware back-press handling

Back press must be offered to the instance that currently owns the screen. With more than one live
instance, track which one is in the foreground and delegate to that one:

```ts
BackHandler.addEventListener('hardwareBackPress', () => {
  const active = this.activeInstance;
  return !!active && !active.isNull() && active.onBackPressed();
});
```

### Step-6: Android permissions and activity results

Unchanged and still done once, at the activity level — the snippets in
[Step-6](#step-6-android-permissions-handling) of the single-instance guide apply as-is. No
per-instance wiring is required in `MainActivity`.

Permission and activity results are offered to every live instance; the SDK routes them internally by
request code, so no per-instance wiring is needed.

### Step-7: Terminate

Terminate each instance you created. Terminating one instance does not affect the others.

```ts
instance.terminate();
```

After `terminate()` the key is released natively; also remove the JS listener registered in
[Step-3](#step-3-listen-to-events-from-this-instance) and drop your reference to the object, otherwise
the instance is retained on the JS side.

### Helpers

```ts
const isNull: boolean = instance.isNull();          // native object missing / already terminated
instance.isInitialised().then((init: boolean) => {}); // initiate has completed
const key: string = instance.getHyperEventString();   // event channel for this instance
```

### Instance API

```ts
class HyperServiceInstance {
  constructor(tenantId?: string, clientId?: string);
  initiate(data: string): void;
  process(data: string): void;
  processWithActivity(data: string): void;
  openPaymentPage(data: string): void;
  terminate(): void;
  onBackPressed(): boolean;
  isNull(): boolean;
  isInitialised(): Promise<boolean>;
  notifyAboutRegisterComponent(viewType: string, componentName?: string): void;
  updateMerchantViewHeight(tag: string, height: number): void;
  getHyperEventString(): string;
}
```

### Optional: Merchant views per instance

Each instance can register its own merchant view components, so two instances can render different
headers or footers. Register the component with `AppRegistry` under a name of your choice, then tell
the instance which name to use for which slot:

```ts
AppRegistry.registerComponent('HeaderForTenantA', () => TenantAHeader);
instanceA.notifyAboutRegisterComponent(HyperSdkReact.JuspayHeader, 'HeaderForTenantA');

AppRegistry.registerComponent('HeaderForTenantB', () => TenantBHeader);
instanceB.notifyAboutRegisterComponent(HyperSdkReact.JuspayHeader, 'HeaderForTenantB');
```

Call it before `initiate`. If `componentName` is omitted, the view tag itself is used, matching the
single-instance behaviour. Components registered process-wide via
`HyperSdkReact.notifyAboutRegisterComponent()` act as the fallback when an instance has no
registration of its own.

On iOS, report the component's height through the instance (Android measures the view itself):

```ts
instance.updateMerchantViewHeight('HeaderForTenantA', height);
```

### Optional: HyperFragmentView per instance

Pass the instance to `HyperFragmentView` to render a fragment from that instance; omit it for the
single-instance API:

```tsx
<HyperFragmentView
  height={300}
  namespace="hyperpay"
  payload={JSON.stringify(processPayload)}
  instance={instanceA}
/>
```

### Limitations

- **`processWithActivity`** opens one full-screen activity per call; each instance's flow now owns its
  own activity and back-press handling, but launching two full-screen flows at the same time stacks
  the activities, so run one visible flow at a time. On iOS it behaves exactly like `process`.
- **Permission and activity results** are offered to every live instance and routed internally by
  request code. If two instances trigger flows that wait on the same Android request code at the same
  moment, results cannot be disambiguated — avoid two simultaneous permission-driven flows.
- The single-instance `HyperSdkReact.*` API and `HyperServiceInstance` can coexist in one app; the
  process-wide `notifyAboutRegisterComponent` registrations act as fallbacks for instances without
  their own.

## Payload Structure

Please refer [here for Express Checkout SDK](https://developer.juspay.in/v2.0/docs/payload) and [here for Payment Page SDK](https://developer.juspay.in/v4.0/docs/payload), for all request and response payload structure.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

hyper-sdk-react is distributed under [Apache 2.0 ](https://github.com/juspay/hyper-sdk-react/blob/main/LICENSE.md) license.
