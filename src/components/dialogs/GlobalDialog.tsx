import { useDataStore } from '../../zustand/app_data_store';
import { Dialog, DialogButton } from 'konsta/react';

const GlobalDialog = () => {
  const { globalDialogData, setglobalDialogData } = useDataStore();
  const handleClose = () => {
    if (globalDialogData.onClose) {
      globalDialogData.onClose();
    } else {
      setglobalDialogData({ ...globalDialogData, isOpen: false });
    }
  };

  return (
    <Dialog
      className="w-9/10"
      opened={globalDialogData.isOpen}
      onBackdropClick={handleClose}
      title={globalDialogData.title ? globalDialogData.title : ''}
      content={
        globalDialogData.subtitle
          ? globalDialogData.subtitle
          : globalDialogData.children
            ? globalDialogData.children
            : ''
      }
      buttons={
        <>
          {globalDialogData.cancel && (
            <DialogButton
              onClick={() => {
                globalDialogData.cancel?.onCancel
                  ? globalDialogData.cancel?.onCancel()
                  : handleClose();
              }}
            >
              {globalDialogData.cancel.cancelText}
            </DialogButton>
          )}

          {globalDialogData.ok && (
            <DialogButton
              strong
              onClick={() => {
                globalDialogData.ok?.onOk ? globalDialogData.ok?.onOk() : handleClose();
              }}
            >
              {globalDialogData.ok.okText}
            </DialogButton>
          )}
        </>
      }
    />
  );
};

export default GlobalDialog;
