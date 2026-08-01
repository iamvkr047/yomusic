import { useEffect, useMemo, useState } from 'react';
import {
  MdAdd,
  MdChevronLeft,
  MdChevronRight,
  MdFastForward,
  MdFastRewind,
  MdFavorite,
  MdFavoriteBorder,
  MdLoop,
  MdPause,
  MdPlayArrow,
  MdVideocam,
} from 'react-icons/md';
import { formatTime } from '../../utils/formatTime';
import { useDataStore } from '../../zustand/app_data_store';
import type { VideoType } from '../../types';
import { Range } from 'konsta/react';
import { toastMsg } from '../../utils/toastMsg';
import { playVideo } from '../../playVideo';

const MusicControls = ({ data }: { data: VideoType }) => {
  const { videoId, duration } = data;

  const {
    favourites,
    setFavourites,
    globalQueue,
    setGlobalQueue,
    playlists,
    setPlaylists,
    globalDialogData,
    setglobalDialogData,
    isPlaying,
    setIsPlaying,
    setIsPlayerVisible,
    progress,
    seekFn,
    isLoop,
    setIsLoop,
  } = useDataStore();

  const [freeProg, setFreeProg] = useState(progress);
  useEffect(() => {
    setFreeProg(progress);
  }, [progress]);

  useEffect(() => {
    // get loop value from local storage:
    const isLoopStr = localStorage.getItem('isLoop'); // "true" || "false" || null
    if (!isLoopStr) {
      setIsLoop(false);
    } else {
      setIsLoop(isLoopStr === 'true');
    }
  }, []);

  const isAlreadyFav = useMemo(
    () => favourites.some((item) => item.videoId === videoId),
    [favourites, videoId]
  );

  const handleDragEnd = () => {
    seekFn(freeProg);
  };

  const handleAddToClist = () => {
    const list = playlists.filter((item) => item.channel === 'Custom list');
    if (list.length <= 0) {
      toastMsg('Playlist not Found! Create new');
      return false;
    }
    setglobalDialogData({
      isOpen: true,
      title: 'Add',
      children: (
        <div className="max-h-80 overflow-y-auto mt-2">
          <p className="text-xl">Select a Playlist to add</p>
          {list.map((pList, i) => (
            <button
              key={i}
              className="px-2 py-4 justify-start my-2 bg-surface-variant rounded-xl block w-full text-start"
              onClick={() => {
                const copyPlaylists = [...playlists];
                const targetIndex = copyPlaylists.findIndex(
                  (c) => c.playlistId === pList.playlistId
                );
                if (targetIndex >= 0) {
                  // check if already exist:
                  if (
                    copyPlaylists[targetIndex].videoItems.items.some((s) => s.videoId === videoId)
                  ) {
                    // has already
                    toastMsg('Item Already Exist');
                    return false;
                  }
                  copyPlaylists[targetIndex].videoItems.items.push(data); // data from props
                  setPlaylists(copyPlaylists);
                  toastMsg('Item Added');
                }
                // close dialog
                setglobalDialogData({ ...globalDialogData, isOpen: false });
              }}
            >
              {pList.title}
            </button>
          ))}
        </div>
      ),
    });
  };

  const handleAddToFav = () => {
    setFavourites([data, ...favourites]);
    toastMsg('Added to Liked Songs');
  };

  const handleRemoveFav = () => {
    setFavourites(favourites.filter((item) => item.videoId !== videoId));
    toastMsg('Removed from Liked Songs');
  };

  const handlePlayPrev = () => {
    if (globalQueue.contents.length <= 0 || globalQueue.currentIndex <= 0) {
      return false;
    }
    // navigate('/play/' + globalQueue.contents[globalQueue.currentIndex - 1].videoId, {
    //   replace: true,
    // });
    playVideo(globalQueue.contents[globalQueue.currentIndex - 1].videoId);
    // update global queue too:
    setGlobalQueue({
      ...globalQueue,
      currentIndex: globalQueue.currentIndex - 1,
    });
  };

  const handlePlayNext = () => {
    if (
      globalQueue.contents.length <= 0 ||
      globalQueue.currentIndex >= globalQueue.contents.length - 1
    ) {
      return false;
    }
    // navigate('/play/' + globalQueue.contents[globalQueue.currentIndex + 1].videoId, {
    //   replace: true,
    // });
    playVideo(globalQueue.contents[globalQueue.currentIndex + 1].videoId);
    // update global queue too:
    setGlobalQueue({
      ...globalQueue,
      currentIndex: globalQueue.currentIndex + 1,
    });
  };

  return (
    <div>
      <div className="flex py-4 gap-4 w-full justify-evenly">
        <MdVideocam
          className="size-8 cursor-pointer hover:scale-110"
          onClick={() => {
            setIsPlayerVisible(true);
          }}
        />
        <MdAdd className="size-8 cursor-pointer hover:scale-110" onClick={handleAddToClist} />
        {isAlreadyFav ? (
          <MdFavorite className="size-8 cursor-pointer hover:scale-110" onClick={handleRemoveFav} />
        ) : (
          <MdFavoriteBorder
            className="size-8 cursor-pointer hover:scale-110"
            onClick={handleAddToFav}
          />
        )}
        {/* <MdDownload
        className="size-8 cursor-pointer hover:scale-110"
        onClick={() => {
        //   window.open(`https://mp3api.ytjar.info?id=${vid}`, "_blank")
        }}
      /> */}
        <MdLoop
          className={`size-8 cursor-pointer hover:scale-110 rotate-145 ${
            isLoop ? '' : 'text-[#656565]'
          }`}
          onClick={() => {
            // toggle Loop:
            setIsLoop(!isLoop);
            localStorage.setItem('isLoop', isLoop ? 'false' : 'true');
          }}
        />
      </div>

      <Range
        className="range-input-fix"
        value={freeProg}
        step={1}
        min={0}
        max={duration}
        onChange={(e) => {
          setFreeProg(Number(e.target.value));
        }}
        onMouseUp={handleDragEnd}
        onTouchEnd={handleDragEnd}
      />

      <style>{`
      
        `}</style>
      <div className="flex justify-between items-center mt-1 text-sm font-semibold">
        <span>{formatTime(progress)}</span>
        <span>{formatTime(duration)}</span>
      </div>
      <div className="flex py-4 gap-4 w-full justify-evenly">
        <MdChevronLeft className="size-8 cursor-pointer hover:scale-110" onClick={handlePlayPrev} />
        <MdFastRewind
          className="size-8 cursor-pointer hover:scale-110"
          onClick={() => {
            seekFn(freeProg - 10);
          }}
        />
        {!isPlaying ? (
          <MdPlayArrow
            className="size-8 cursor-pointer hover:scale-110"
            onClick={() => {
              setIsPlaying(true);
            }}
          />
        ) : (
          <MdPause
            className="size-8 cursor-pointer hover:scale-110"
            onClick={() => {
              setIsPlaying(false);
            }}
          />
        )}
        <MdFastForward
          className="size-8 cursor-pointer hover:scale-110"
          onClick={() => {
            seekFn(freeProg + 10);
          }}
        />
        <MdChevronRight
          className="size-8 cursor-pointer hover:scale-110"
          onClick={handlePlayNext}
        />
      </div>
    </div>
  );
};

export default MusicControls;
