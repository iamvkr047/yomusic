import { MdArchive } from 'react-icons/md';
import { useDataStore } from '../../zustand/app_data_store';
import { Toast } from '@capacitor/toast';
import { Share } from '@capacitor/share';
import { Filesystem, Directory, Encoding } from '@capacitor/filesystem';
import { isValidPlaylistArray, isValidVideoArray } from '../../types';
import { ListItem } from 'konsta/react';

const ImportExport = () => {
  const {
    globalDialogData,
    setglobalDialogData,
    playlists,
    favourites,
    recentSearches,
    setPlaylists,
    setFavourites,
    setRecentSearches,
  } = useDataStore();

  function showImportExport() {
    setglobalDialogData({
      isOpen: true,
      title: 'Import Export',
      subtitle: 'Import or export your data',
      ok: {
        okText: 'Import',
        onOk: () => {
          document.getElementById('fileBox')?.click();
        },
      },
      cancel: {
        cancelText: 'Export',
        onCancel: () => {
          exportData();
        },
      },
    });
  }

  const exportData = async () => {
    const jsonString = JSON.stringify({ playlists, favourites, recentSearches }, null, 2);

    await Filesystem.writeFile({
      path: 'Export.ymb',
      data: jsonString,
      directory: Directory.Cache,
      encoding: Encoding.UTF8,
    });

    Share.share({
      url: (
        await Filesystem.getUri({
          path: 'Export.ymb',
          directory: Directory.Cache,
        })
      ).uri,
    });
  };

  const importData = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files) {
      return false;
    }
    const f = e.target.files[0];

    if (!f.name.trim().toLowerCase().includes('.ymb')) {
      Toast.show({
        text: 'Invalid File',
      });
      return false;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result;
      if (!result) return false;
      try {
        const { playlists, favourites, recentSearches } = JSON.parse(result as string);
        // fav validation
        const isValidfav = isValidVideoArray(favourites);

        // recentSearches validation
        const isValidrecentSearches = isValidVideoArray(recentSearches);

        // playlist validation
        const isValidPlaylist = isValidPlaylistArray(playlists);

        // save this data locally:
        if (isValidPlaylist) setPlaylists(playlists);
        if (isValidfav) setFavourites(favourites);
        if (isValidrecentSearches) setRecentSearches(recentSearches);

        showResultDialog({
          isValidfav,
          isValidrecentSearches,
          isValidPlaylist,
        });
      } catch (error) {
        Toast.show({
          text: 'Invalid Format',
        });
      } finally {
        (document.getElementById('fileBox') as HTMLInputElement).value = '';
      }
    };
    reader.readAsText(f);
  };

  const showResultDialog = ({
    isValidfav,
    isValidrecentSearches,
    isValidPlaylist,
  }: {
    isValidfav: boolean;
    isValidrecentSearches: boolean;
    isValidPlaylist: boolean;
  }) => {
    setglobalDialogData({
      isOpen: true,
      title: 'Import Export',
      children: (
        <div>
          <p>{isValidfav ? '✔' : '✖'} Favorites</p>
          <p>{isValidPlaylist ? '✔' : '✖'} Playlists</p>
          <p>{isValidrecentSearches ? '✔' : '✖'} Recent Searches</p>
        </div>
      ),
      ok: {
        okText: 'Ok',
        onOk: () => {
          setglobalDialogData({ ...globalDialogData, isOpen: false });
        },
      },
    });
  };

  return (
    <>
      <ListItem
        media={<MdArchive size={24} />}
        onClick={showImportExport}
        link
        title="Your Data"
        chevronMaterial={false}
        contentClassName="bg-md-light-surface-3 dark:bg-md-dark-surface-3 m-0! rounded-lg_"
      />
      <input
        id="fileBox"
        hidden={true}
        type="file"
        name="importFile"
        accept=".ymb"
        onChange={importData}
      />
    </>
  );
};

export default ImportExport;
