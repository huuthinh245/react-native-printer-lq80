import * as React from 'react';

import { StyleSheet, View, Text,  TouchableOpacity } from 'react-native';
import PrinterLq80 from 'react-native-printer-lq80';
import { launchImageLibrary, MediaType } from 'react-native-image-picker';

const options = {
  mediaType: 'photo' as MediaType,
  includeBase64: true,
  // maxHeight: 639,
  // maxWidth: 639,
} 

export default function App() {
  const [connected, setConnected] = React.useState<boolean>(false);
  const [filePath, setFilePath] = React.useState("");
  React.useEffect(() => {
    PrinterLq80.init();
    // PrinterLq80.multiply(3, 7).then(setResult);
  }, []);

  const connectUsb = async () => {
    try {
      const connected = await PrinterLq80.connectUSB();
      setConnected(connected)
    } catch (error) {
      
    }
  }

  const checkPrintStatus = () => {
    try {
      PrinterLq80.getPrintStatus()
    } catch (error) {
      
    }
  }

  const printImage = () => {
    if(filePath) {
      PrinterLq80.printImage(filePath)
    }
  }

  const _handleButtonPress = () => {
    launchImageLibrary(options,(response) => {   
      if(response?.base64) {
          setFilePath(response.base64)
      }else {
         
      }
  })
    };
  return (
    <View style={styles.container}>
      <Text>Result</Text>
      <TouchableOpacity onPress={connectUsb}>
          <Text>connect: {`${connected}`}</Text>
      </TouchableOpacity>
      <TouchableOpacity onPress={checkPrintStatus} style={{ marginTop: 20 }}>
          <Text>check print status</Text>
      </TouchableOpacity>
      <TouchableOpacity onPress={_handleButtonPress} style={{ marginTop: 20 }}>
          <Text>get photo</Text>
      </TouchableOpacity>
      <TouchableOpacity onPress={printImage} style={{ marginTop: 20 }}>
          <Text>print image</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
