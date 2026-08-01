import { useEffect, useState } from 'react';
import { MdCloudDownload } from 'react-icons/md';
import { checkForUpdate } from '../utils/checkForUpdate';
import { Icon, Link } from 'konsta/react';

const UpdateBlock = () => {
  const [isupdateAvailable, setisupdateAvailable] = useState(false);
  useEffect(() => {
    checkForUpdate().then((data) => {
      if (data && data.version) {
        setisupdateAvailable(true);
      }
    });
  }, []);

  return (
    isupdateAvailable && (
      <Link iconOnly href="https://yomusic.vercel.app" target="_blank">
        <Icon
          material={<MdCloudDownload className="size-6" />}
          badge=""
          badgeColors={{ bg: 'bg-red-500' }}
        />
      </Link>
    )
  );
};

export default UpdateBlock;
