import { MdInfo } from 'react-icons/md';
import { useDataStore } from '../../zustand/app_data_store';
import { getCurrentAppVersion } from '../../utils/getCurrentAppVersion';
import { ListItem } from 'konsta/react';

const About = () => {
  const { setglobalDialogData } = useDataStore();

  async function showAbout() {
    const v = await getCurrentAppVersion();
    if (v > 0) {
      setglobalDialogData({
        isOpen: true,
        title: 'About',
        ok: {
          okText: 'Ok',
        },
        cancel: {
          cancelText: 'Cancel',
        },
        children: (
          <div className="text-sm flex flex-col gap-4">
            <h2>App Version: {v.toFixed(1)} </h2>
            <h4>
              Hi, this app is created by iamvkr. I am an aspring app and web application developer,
              crafting products and solutions.
            </h4>

            <p>Need a similar products or solutions?</p>
            <p>
              Contact me Via{' '}
              <a className="underline" href="mailto:beecoder260@proton.me">
                mail
              </a>
            </p>
            <p>
              Know More:{' '}
              <a className="underline" href="https://github.com/iamvishalkr">
                Github
              </a>
            </p>
          </div>
        ),
      });
    }
  }
  return (
    <ListItem
      media={<MdInfo size={24} />}
      onClick={showAbout}
      link
      title="About"
      chevronMaterial={false}
      contentClassName="bg-md-light-surface-3 dark:bg-md-dark-surface-3 m-0!"
    />
  );
};

export default About;
