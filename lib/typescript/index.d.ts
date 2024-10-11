export { default as HyperFragmentView } from './HyperFragmentView';
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
    openPaymentPage(data: string): void;
    updateMerchantViewHeight(tag: string, height: number): void;
    notifyAboutRegisterComponent(tag: string): void;
    JuspayHeader: string;
    JuspayHeaderAttached: string;
    JuspayFooter: string;
    JuspayFooterAttached: string;
};
declare const _default: HyperSdkReactType;
export default _default;
//# sourceMappingURL=index.d.ts.map