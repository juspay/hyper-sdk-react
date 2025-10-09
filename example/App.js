import React, {useEffect, useState} from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  Alert,
  useColorScheme,
} from 'react-native';

// Import HyperSDK
import HyperSdkReact, { HyperFragmentView } from 'hyper-sdk-react';
import { NativeEventEmitter, NativeModules } from 'react-native';

console.log('🔍 [App] HyperSdkReact imported:', !!HyperSdkReact);
console.log('🔍 [App] HyperFragmentView imported:', !!HyperFragmentView);

function App() {
  const isDarkMode = useColorScheme() === 'dark';
  const [sdkInitialized, setSdkInitialized] = useState(false);
  const [paymentStatus, setPaymentStatus] = useState('Ready');
  const [processPayload, setProcessPayload] = useState(''); // Add this for HyperFragmentView
  
  console.log('🔍 [App] Component rendering - processPayload length:', processPayload.length);
  console.log('🔍 [App] Component rendering - processPayload isEmpty:', processPayload === '');

  const backgroundStyle = {
    backgroundColor: isDarkMode ? '#000' : '#fff',
    flex: 1,
  };

  // UUID generator function
  const generateUUID = () => {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  };

  useEffect(() => {
    console.log('🚀 App started - Initializing HyperSDK...');
    
    // Check and log architecture info
    try {
      const archInfo = HyperSdkReact.getArchitectureInfo();
      console.log('🏗️ [Architecture Detection]', archInfo);
      setPaymentStatus('Architecture: ' + archInfo.split(':')[1].trim());
    } catch (error) {
      console.log('❌ [Architecture Detection] Error:', error);
      setPaymentStatus('Architecture detection failed');
    }
    
    // Set up event listener for HyperSDK events
    const eventEmitter = new NativeEventEmitter(NativeModules.HyperSdkReact);
    const eventListener = eventEmitter.addListener('HyperEvent', (resp) => {
      console.log('🎉 HyperEvent received:', resp);
      try {
        const data = JSON.parse(resp);
        console.log('📊 Parsed HyperEvent:', JSON.stringify(data, null, 2));
        
        switch (data.event) {
          case 'initiate_result':
            if (data.error) {
              console.error('❌ HyperSDK initiate error:', data.errorMessage);
              setSdkInitialized(false);
              setPaymentStatus('Error: ' + data.errorMessage);
            } else {
              console.log('✅ HyperSDK initiate successful');
              setSdkInitialized(true);
              setPaymentStatus('SDK Initialized Successfully');
              
              // Auto-create payment session with HyperFragmentView support
              fetch('https://sandbox.juspay.in/session', {
                method: 'POST',
                headers: {
                  'Authorization': 'Basic OTNBQTlFNzdCMzA0NzkwQUI5NUVBOEE4NjMwMDcxOg==',
                  'x-merchantid': 'picasso',
                  'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                  order_id: Date.now().toString(),
                  amount: '1.0',
                  customer_id: 'testing-customer-one',
                  customer_email: 'test@mail.com',
                  customer_phone: '9876543210',
                  payment_page_client_id: 'instaastro',
                  action: 'paymentPage',
                  return_url: 'https://youtube.com',
                  description: 'Complete your payment',
                  first_name: 'John',
                  last_name: 'wick',
                  features: { paymentWidget: { enable: true } }, // Enable payment widget
                }),
              })
              .then(response => response.json())
              .then(data => {
                console.log('Session API response:', data.sdk_payload);
                setProcessPayload(JSON.stringify(data.sdk_payload)); // Set payload for HyperFragmentView
              })
              .catch(error => {
                console.error('Session API error:', error);
              });
            }
            break;
          case 'process_result':
            console.log('💳 Payment process result:', data);
            setPaymentStatus('Payment Completed');
            break;
          default:
            console.log('📱 HyperSDK Event:', data.event, data);
        }
      } catch (error) {
        console.error('❌ Error parsing HyperEvent:', error);
        setPaymentStatus('Error parsing event');
      }
    });
    
    // Initialize HyperSDK after a short delay
    const initializeHyperSDK = async () => {
      try {
        console.log('📱 Creating HyperSDK services...');
        setPaymentStatus('Creating services...');
        
        // Create Hyper services first
        await HyperSdkReact.createHyperServices();
        console.log('✅ HyperSDK services created');
        
        setPaymentStatus('Initializing SDK...');
        
        const initiatePayload = {
          requestId: generateUUID(),
          service: 'in.juspay.hyperpay',
          payload: {
            action: 'initiate',
            merchantId: 'picasso', // Updated to match your setup
            clientId: 'instaastro', // Updated to match your setup
            environment: 'sandbox',
          },
        };
        
        console.log('🚀 HyperSDK initiate payload:', JSON.stringify(initiatePayload, null, 2));
        await HyperSdkReact.initiate(JSON.stringify(initiatePayload));
        console.log('✅ HyperSDK initiate called');
        
      } catch (error) {
        console.error('❌ HyperSDK initialization failed:', error);
        setPaymentStatus('SDK Init Failed: ' + error.message);
      }
    };
    
    // Initialize after component mounts
    setTimeout(initializeHyperSDK, 2000);
    
    // Cleanup function
    return () => {
      eventListener.remove();
      console.log('🧹 HyperSDK event listener cleaned up');
    };
  }, []);

  const startPayment = async () => {
    try {
      console.log('💳 Starting payment process...');
      setPaymentStatus('Processing Payment...');
      
      const payload = {
        "requestId": generateUUID(),
        "service": "in.juspay.hyperpay",
        "payload": {
          "action": "paymentPage",
          "clientId": "instaastro",
          "merchantId": "picasso",
          "clientAuthToken": "tkn_6d39490435174c3b8c88ca331f954ce6",
          "clientAuthTokenExpiry": "2025-12-31T23:59:59Z",
          "environment": "sandbox",
          "orderId": "order_" + Date.now(),
          "amount": "100.0",
          "currency": "INR",
          "customerId": "testing-customer-one",
          "customerEmail": "test@mail.com",
          "customerPhone": "9876543210",
          "firstName": "John",
          "lastName": "Wick",
          "returnUrl": "https://google.com",
          "description": "Complete your payment"
        }
      };
      
      console.log('📋 Payment payload:', JSON.stringify(payload, null, 2));
      await HyperSdkReact.process(JSON.stringify(payload));
      console.log('✅ Payment process initiated');
      
    } catch (error) {
      console.error('❌ Payment failed:', error);
      setPaymentStatus('Payment Failed: ' + error.message);
      Alert.alert('Payment Error', error.message);
    }
  };

  return (
    <SafeAreaView style={backgroundStyle}>
      <StatusBar
        barStyle={isDarkMode ? 'light-content' : 'dark-content'}
        backgroundColor={backgroundStyle.backgroundColor}
      />
      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        style={backgroundStyle}>
        <View style={styles.container}>
          <Text style={styles.title}>HyperSDK React Native</Text>
          
          <View style={styles.statusContainer}>
            <Text style={styles.statusLabel}>Status:</Text>
            <Text style={[styles.status, {color: sdkInitialized ? 'green' : 'orange'}]}>
              {paymentStatus}
            </Text>
          </View>
          
          <View style={styles.infoContainer}>
            <Text style={styles.infoText}>✅ React Native 0.81.4</Text>
            <Text style={styles.infoText}>✅ New Architecture (Fabric + TurboModules)</Text>
            <Text style={styles.infoText}>✅ HyperSDK v4.0.6</Text>
            <Text style={styles.infoText}>✅ Hermes Engine</Text>
            <Text style={styles.infoText}>✅ HyperFragmentView Support</Text>
          </View>
          
          <TouchableOpacity 
            style={[styles.button, !sdkInitialized && styles.buttonDisabled]} 
            onPress={startPayment}
            disabled={!sdkInitialized}>
            <Text style={styles.buttonText}>
              {sdkInitialized ? 'Start Payment (₹100)' : 'SDK Initializing...'}
            </Text>
          </TouchableOpacity>
          
          {/* HyperFragmentView - Shows when payment widget is ready */}
          {processPayload !== '' && (
            <View style={styles.fragmentContainer}>
              <Text style={styles.fragmentTitle}>Payment Widget (With Payload):</Text>
              <View style={styles.fragmentWrapper}>
                {console.log('🔍 [App] Rendering HyperFragmentView with payload length:', processPayload.length)}
                <HyperFragmentView
                  height={103}
                  width={300}
                  payload={processPayload}
                  namespace={'paymentWidget'}
                />
              </View>
            </View>
          )}
          
          {/* TEST: Always render HyperFragmentView to debug */}
          {/* <View style={styles.fragmentContainer}>
            <Text style={styles.fragmentTitle}>Test HyperFragmentView (Always Rendered):</Text>
            <View style={[styles.fragmentWrapper, { borderColor: 'orange' }]}>
              {console.log('🔍 [App] Rendering TEST HyperFragmentView')}
              <HyperFragmentView
                height={103}
                width={300}
                payload={'{"test": "payload"}'}
                namespace={'testWidget'}
              />
            </View>
          </View> */}
          
          <Text style={styles.note}>
            Check console logs for detailed HyperSDK integration information
          </Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
    marginTop: 50,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 10,
    textAlign: 'center',
    color: '#007AFF',
  },
  subtitle: {
    fontSize: 16,
    marginBottom: 30,
    textAlign: 'center',
    color: '#666',
  },
  statusContainer: {
    marginBottom: 30,
    alignItems: 'center',
  },
  statusLabel: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 5,
  },
  status: {
    fontSize: 14,
    textAlign: 'center',
    fontWeight: '500',
  },
  infoContainer: {
    marginBottom: 30,
    alignItems: 'center',
  },
  infoText: {
    fontSize: 14,
    marginBottom: 5,
    color: '#4CAF50',
  },
  button: {
    backgroundColor: '#007AFF',
    paddingHorizontal: 30,
    paddingVertical: 15,
    borderRadius: 8,
    marginBottom: 20,
    minWidth: 200,
  },
  buttonDisabled: {
    backgroundColor: '#ccc',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
    textAlign: 'center',
  },
  fragmentContainer: {
    marginTop: 20,
    marginBottom: 20,
    alignItems: 'center',
  },
  fragmentTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 10,
    color: '#007AFF',
  },
  fragmentWrapper: {
    backgroundColor: '#f0f0f0',
    borderRadius: 8,
    padding: 10,
    borderWidth: 2,
    borderColor: '#007AFF',
  },
  note: {
    fontSize: 12,
    color: '#999',
    textAlign: 'center',
    fontStyle: 'italic',
  },
});

export default App;
