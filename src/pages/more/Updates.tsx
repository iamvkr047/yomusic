import { MdCloudDownload } from 'react-icons/md';
import { useDataStore } from '../../zustand/app_data_store';
import { checkForUpdate } from '../../utils/checkForUpdate';
import { ListItem } from 'konsta/react';

const Updates = () => {
  const { setglobalDialogData } = useDataStore();
  const UPDATE_URL = `https://yomusic.vercel.app`;
  const closeDialog = () => {
    setglobalDialogData(cleanDialogProps);
  };
  function showUpdateDialog() {
    setglobalDialogData({
      isOpen: true,
      title: 'Updates',
      subtitle: 'check for updates',
      ok: {
        okText: 'Ok',
        onOk: handleCheckUpdate,
      },
      cancel: {
        cancelText: 'Cancel',
        onCancel: closeDialog,
      },
    });
  }

  const handleCheckUpdate = async () => {
    setglobalDialogData({
      ...cleanDialogProps,
      isOpen: true,
      subtitle: 'Checking for updates...',
    });

    const isUpdateAvailable = await checkForUpdate();
    if (isUpdateAvailable) {
      setglobalDialogData({
        ...cleanDialogProps,
        isOpen: true,
        subtitle: 'New Update Available',
        ok: {
          okText: 'Update',
          onOk: () => {
            window.open(UPDATE_URL, '_blank');
          },
        },
        cancel: {
          cancelText: 'Cancel',
          onCancel: closeDialog,
        },
      });
    } else {
      setglobalDialogData({
        ...cleanDialogProps,
        isOpen: true,
        subtitle: 'Already using latest version',
        ok: {
          okText: 'Ok',
          onOk: closeDialog,
        },
        cancel: {
          cancelText: 'Cancel',
          onCancel: closeDialog,
        },
      });
    }
  };
  return (
    <ListItem
      media={<MdCloudDownload size={24} />}
      onClick={showUpdateDialog}
      link
      title="Updates"
      chevronMaterial={false}
      contentClassName="bg-md-light-surface-3 dark:bg-md-dark-surface-3 m-0! rounded-lg_"
    />
  );
};

export default Updates;

/** UTILITY CLEAN DIALOG PROPS */
const cleanDialogProps = {
  isOpen: false,
  title: 'Updates',
  subtitle: '',
  onClose: () => {},
  ok: {
    okText: 'Ok',
    onOk: () => {},
  },
  cancel: {
    cancelText: 'Cancel',
    onCancel: () => {},
  },
};
