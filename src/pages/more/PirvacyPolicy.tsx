import { MdPolicy } from 'react-icons/md';
import { useDataStore } from '../../zustand/app_data_store';
import Priv_Pol_Component from '../../components/Priv_Pol_Component';
import { ListItem } from 'konsta/react';

const PirvacyPolicy = () => {
  const { setglobalDialogData, globalDialogData } = useDataStore();

  const closeDialog = () => {
    setglobalDialogData({ ...globalDialogData, isOpen: false });
  };
  function openPrivPolicy() {
    setglobalDialogData({
      isOpen: true,
      title: 'Privacy Policy',
      ok: {
        okText: 'Ok',
        onOk: closeDialog,
      },
      cancel: {
        cancelText: 'Cancel',
        onCancel: closeDialog,
      },
      children: (
        <div className="max-h-80 text-xs overflow-y-auto">
          <Priv_Pol_Component />
        </div>
      ),
    });
  }
  return (
    <ListItem
      media={<MdPolicy size={24} />}
      onClick={openPrivPolicy}
      link
      title="Privacy Policy"
      chevronMaterial={false}
      contentClassName="bg-md-light-surface-3 dark:bg-md-dark-surface-3 m-0! rounded-b-lg"
    />
  );
};

export default PirvacyPolicy;
