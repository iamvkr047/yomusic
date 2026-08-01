import { StatusBar, Style } from '@capacitor/status-bar';
import { useEffect, useState } from 'react';
import { MdDarkMode, MdSunny } from 'react-icons/md';
import { Device } from '@capacitor/device';
import { Link } from 'konsta/react';

const DarkModeToggle = () => {
  const [theme, setTheme] = useState(localStorage.getItem('theme'));

  const toggleTheme = () => {
    const next = theme === 'dark' ? 'light' : 'dark';
    setTheme(next);
    localStorage.setItem('theme', next);
    if (theme) {
      document.documentElement.classList.remove(theme);
    }
    document.documentElement.classList.add(next);
    // status bar update with new theme:
    updateSystemStatusBars(next === 'dark');
  };

  const updateSystemStatusBars = async (isDarkMode: boolean) => {
    try {
      const info = await Device.getInfo(); // chanage statubar only for android 14 and less:
      if (isDarkMode && info.androidSDKVersion && info.androidSDKVersion <= 34) {
        // Style.Dark makes the status bar text/icons LIGHT (WHITE)
        await StatusBar.setStyle({ style: Style.Dark });
      } else {
        // Style.Light makes the status bar text/icons DARK (BLACK)
        await StatusBar.setStyle({ style: Style.Light });
      }
    } catch (error) {
      console.error('StatusBar plugin error:', error);
    }
  };

  useEffect(() => {
    // at the time of running this useeffect we are 100% sure get theme value from local storage.
    // so update statusbar text color:
    const stored = localStorage.getItem('theme');
    updateSystemStatusBars(stored === 'dark');
  }, []);

  return theme === 'light' ? (
    <Link iconOnly>
      <MdDarkMode onClick={toggleTheme} className="size-7" />
    </Link>
  ) : (
    <Link iconOnly>
      <MdSunny onClick={toggleTheme} className="size-7" />
    </Link>
  );
};

export default DarkModeToggle;
