import { NativeModules } from 'react-native';

type PrinterLq80Type = {
  multiply(a: number, b: number): Promise<number>;
};

const { PrinterLq80 } = NativeModules;

export default PrinterLq80 as PrinterLq80Type;
