import type { PlaylistType, VideoType } from '../../types';
import { useDataStore } from '../../zustand/app_data_store';
import { ListItem } from 'konsta/react';
import { useLongPress } from '../../hooks/useLongPress';
import { toastMsg } from '../../utils/toastMsg';
import { playVideo } from '../../playVideo';

const VideoCard = ({ video, playlistData }: { video: VideoType; playlistData: PlaylistType }) => {
  const { setGlobalQueue, globalDialogData, setglobalDialogData, playlists, setPlaylists } =
    useDataStore();

  const closeDialog = () => {
    setglobalDialogData({ ...globalDialogData, isOpen: false });
  };

  const bindLongPress = useLongPress(() => {
    // ONLY ACTIVE FOR VIDEO OF C LIST
    if (playlistData.isCustomChannel) {
      setglobalDialogData({
        isOpen: true,
        title: 'Remove',
        subtitle: 'Are you sure Remove this item?',
        ok: {
          okText: 'Ok',
          onOk: () => {
            const copyPlaylists = [...playlists];
            const targetIndex = copyPlaylists.findIndex(
              (c) => c.playlistId === playlistData.playlistId
            );
            if (targetIndex >= 0) {
              const itemsArr = copyPlaylists[targetIndex].videoItems.items;
              const targetObjIndex = itemsArr.findIndex((f) => f.videoId === video.videoId);
              if (targetObjIndex >= 0) {
                // delete:
                itemsArr.splice(targetObjIndex, 1);
              }
              setPlaylists(copyPlaylists);
              toastMsg('item deleted!');
            }
            // close dialog
            setglobalDialogData({ ...globalDialogData, isOpen: false });
          },
        },
        cancel: {
          cancelText: 'Cancel',
          onCancel: () => {
            closeDialog();
          },
        },
      });
    }
  });

  return (
    <div
      className=""
      {...bindLongPress(`pl-itm-${video.videoId}`)}
      onClick={() => {
        // get  index
        const i = playlistData.videoItems.items.findIndex((item) => item.videoId === video.videoId);
        setGlobalQueue({
          currentIndex: i,
          contents: playlistData.videoItems.items,
        });
        // navigate(`/play/${video.videoId}`);
        playVideo(video.videoId);
      }}
    >
      <ListItem
        contentClassName="bg-md-light-surface-3 dark:bg-md-dark-surface-3 m-0! my-3! rounded-lg"
        titleWrapClassName="line-clamp-2"
        link
        chevronMaterial={false}
        title={video.title}
        subtitle={video.channel}
        media={
          <div
            className="image w-20 h-18 bg-md-light-primary dark:bg-md-dark-primary rounded-lg flex items-center justify-center"
            style={{
              backgroundImage: `url('https://i.ytimg.com/vi/${video.videoId}/mqdefault.jpg')`,
              backgroundSize: 'cover',
              backgroundPosition: 'center',
            }}
          ></div>
        }
      />
    </div>
  );
};

export default VideoCard;
