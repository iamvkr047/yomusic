import { useDataStore } from '../../zustand/app_data_store';
import { useLongPress } from '../../hooks/useLongPress';
import type { VideoType } from '../../types';
import { ListItem } from 'konsta/react';
import { toastMsg } from '../../utils/toastMsg';
import { playVideo } from '../../playVideo';

const FavViewCard = ({ data: { videoId, title, channel } }: { data: VideoType }) => {
  const { favourites, setFavourites, globalDialogData, setglobalDialogData, setGlobalQueue } =
    useDataStore();
  const closeDialog = () => {
    setglobalDialogData({ ...globalDialogData, isOpen: false });
  };
  const bindLongPress = useLongPress(() => {
    setglobalDialogData({
      isOpen: true,
      title: 'Remove',
      subtitle: 'Are You Sure to Remove?',
      ok: {
        okText: 'Ok',
        onOk: () => {
          setFavourites(favourites.filter((data) => data.videoId !== videoId));
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
    <div
      className=""
      {...bindLongPress(`fav-${videoId}`)}
      onClick={() => {
        // navigate(`/play/${videoId}`);
        playVideo(videoId);
        // get  index
        const i = favourites.findIndex((item) => item.videoId === videoId);
        setGlobalQueue({
          currentIndex: i,
          contents: favourites,
        });
      }}
    >
      <ListItem
        contentClassName="bg-md-light-surface-3 dark:bg-md-dark-surface-3 m-0! my-3! rounded-lg"
        titleWrapClassName="line-clamp-2"
        link
        chevronMaterial={false}
        title={title}
        //   after="$15"
        subtitle={channel}
        media={
          <div
            className="image w-20 h-18 bg-md-light-primary dark:bg-md-dark-primary rounded-lg flex items-center justify-center"
            style={{
              backgroundImage: `url('https://i.ytimg.com/vi/${videoId}/mqdefault.jpg')`,
              backgroundSize: 'cover',
              backgroundPosition: 'center',
            }}
          ></div>
        }
      />
    </div>
  );
};

export default FavViewCard;
