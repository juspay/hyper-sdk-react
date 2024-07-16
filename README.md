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
        hyperSDKVersion = "2.1.25"
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
    "hyperSdkIOSVersion": "2.1.39"
    ....
  }
```

Note: This version is just for explanatory purposes and may change in future. Contact Juspay support team for the latest SDK version.

#### **Dynamic Assets iOS**

Change the `hyperSdkIOSVersion` to `2.1.39` (This version is just for explanatory purposes and may change in future. Contact Juspay support team for the latest SDK version).

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
```ts
  onLayout={(event) => {
        const { height } = event.nativeEvent.layout;
        HyperSdkReact.updateMerchantViewHeight(HyperSdkReact.JuspayHeader, height);
      }}

```

## Payload Structure

Please refer [here for Express Checkout SDK](https://developer.juspay.in/v2.0/docs/payload) and [here for Payment Page SDK](https://developer.juspay.in/v4.0/docs/payload), for all request and response payload structure.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

hyper-sdk-react is distributed under [AGPL-3.0-only](https://github.com/juspay/hyper-sdk-react/src/main/LICENSE.md) license.

