import { NativeModules } from 'react-native';

type PrinterLq80Type = {
  init(): void;
  connectUSB(): Promise<boolean>;
  printImage(base64: string): void;
  getPrintStatus(): void
};

const { PrinterLq80 } = NativeModules;

export default PrinterLq80 as PrinterLq80Type;
