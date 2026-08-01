import { getCurrentAppVersion } from './getCurrentAppVersion';

// const BASE_URL = import.meta.env.DEV ? 'http://localhost:5173' : `https://yomusic.vercel.app`;
const URL = `https://api.github.com/repos/iamvkr047/yomusic/releases/latest`;

export const checkForUpdate = async () => {
  const APP_VERSION = await getCurrentAppVersion();
  if (APP_VERSION === 0) {
    return false; // handle web
  }

  const response = await fetch(URL);
  if (response.status !== 200) {
    return false;
  }
  const data = await response.json();
  let parts = data.tag_name.replace('v', '').split('.');
  let version = parseFloat(`${parts[0]}.${parts[1]}`);

  const updateAvailable = APP_VERSION < version;
  if (!updateAvailable) {
    return false;
  }
  return {
    version: updateAvailable ? version : APP_VERSION,
    whatsNew: updateAvailable ? '' : '',
    changeLog: updateAvailable ? data.body : [],
  };
};
