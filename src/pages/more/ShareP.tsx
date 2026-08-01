import { MdShare } from 'react-icons/md';
import { handleShare } from '../../utils/shareText';
import { ListItem } from 'konsta/react';

const ShareP = () => {
  return (
    <ListItem
      media={<MdShare size={24} />}
      onClick={() => {
        handleShare({
          title: 'Download yo Music',
          url: 'https://yomusic.vercel.app',
          text: 'Download yo Music',
        });
      }}
      link
      title="Share"
      chevronMaterial={false}
      contentClassName="bg-md-light-surface-3 dark:bg-md-dark-surface-3 m-0! rounded-lg_"
    />
  );
};

export default ShareP;
