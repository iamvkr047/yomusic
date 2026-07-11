import { useLongPress } from '../../hooks/useLongPress';
import { useDataStore } from '../../zustand/app_data_store';
import type { PlaylistType } from '../../types';
import { useNavigate } from 'react-router-dom';
import { MdPlaylistPlay } from 'react-icons/md';
import { ListItem } from 'konsta/react';
import { toastMsg } from '../../utils/toastMsg';

const PlaylistViewCard = ({ playlistData }: { playlistData: PlaylistType }) => {
  const { playlists, setPlaylists, globalDialogData, setglobalDialogData } = useDataStore();
  const navigate = useNavigate();
  const closeDialog = () => {
    setglobalDialogData({ ...globalDialogData, isOpen: false });
  };
  const bindLongPress = useLongPress(() => {
    setglobalDialogData({
      isOpen: true,
      title: 'Remove',
      subtitle: 'Are you sure Remove this item?',
      ok: {
        okText: 'Ok',
        onOk: () => {
          setPlaylists(playlists.filter((data) => data.playlistId !== playlistData.playlistId));
          toastMsg('item removed');
          closeDialog();
        },
      },
      cancel: {
        cancelText: 'Cancel',
        onCancel: () => {
          closeDialog();
        },
      },
    });
  });
  return (
    <ListItem
      className=""
      {...bindLongPress(`pv-${playlistData.playlistId}`)}
      onClick={() => {
        if (playlistData.isCustomChannel) {
          if (playlistData.videoItems.items.length <= 0) {
            toastMsg('Playlist is empty!');
            return false;
          }
          navigate(`/clist/${playlistData.playlistId}`);
          return true;
        }
        navigate(`/list/${playlistData.playlistId}`);
      }}
      role="button"
      contentClassName="bg-md-light-surface-3 dark:bg-md-dark-surface-3 m-0! my-3!"
      titleWrapClassName="line-clamp-2"
      link
      chevronMaterial={false}
      title={playlistData.title}
      //   after="$15"
      subtitle={playlistData.channel || 'playlist'}
      media={
        <div
          className="image w-20 h-18 bg-md-light-primary dark:bg-md-dark-primary rounded-lg flex items-center justify-center"
          style={{
            backgroundImage: `url(${playlistData.thumbnail})`,
            backgroundSize: 'cover',
            backgroundPosition: 'center',
          }}
        >
          {playlistData.isCustomChannel && (
            <MdPlaylistPlay
              size={32}
              className="text-md-light-on-primary dark:text-md-dark-on-primary"
            />
          )}
        </div>
      }
    />
  );
};

export default PlaylistViewCard;
