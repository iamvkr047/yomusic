import { useEffect, useRef } from 'react';
import useYTPlayer from '../hooks/useYtPlayer';
import { useDataStore } from '../zustand/app_data_store';
import { MdClose } from 'react-icons/md';
import { playVideo } from '../playVideo';

const YtWrapper = () => {
  const {
    currentPlayingVideo,
    setcurrentPlayingVideo,
    isPlaying,
    setIsPlaying,
    setIsPlayerReady,
    isPlayerVisible,
    setProgress,
    setIsPlayerVisible,
    setSeekFn,
    isLoop,
    globalQueue,
    setGlobalQueue,
  } = useDataStore();
  const anchorRef = useRef<HTMLDivElement | null>(null);

  const { seekTo } = useYTPlayer({
    anchorRef,
    videoId: currentPlayingVideo.videoId ?? 'M7lc1UVf-VE',
    isPlaying: isPlaying,
    isLoop: isLoop,
    onReady: () => {
      setIsPlayerReady(true);
    },
    onStateChange: (state) => {
      if (state === 'Playing') {
        setIsPlaying(true);
      } else if (state === 'Paused') {
        setIsPlaying(false);
      }
      if (state === 'Unstarted') {
        setProgress(0);
      }
    },
    onProgress: (current, duration) => {
      setProgress(current);
      if (currentPlayingVideo.duration <= 0) {
        setcurrentPlayingVideo({ ...currentPlayingVideo, duration: duration });
      }
    },
    onEnded: () => {
      // check loop, if not enabled play next in global queue
      if (isLoop) {
        return false;
      }
      if (
        globalQueue.contents.length <= 0 ||
        globalQueue.currentIndex === globalQueue.contents.length - 1 //last item nothing to play next
      ) {
        setIsPlaying(false);
        return false;
      }
      const copyGlobalQueue = { ...globalQueue };
      copyGlobalQueue.currentIndex += 1;
      setGlobalQueue(copyGlobalQueue);
      playVideo(copyGlobalQueue.contents[copyGlobalQueue.currentIndex].videoId);
    },
  });

  useEffect(() => {
    // update global seek to fn so that can be used from anywhere
    setSeekFn(seekTo);
  }, [seekTo]);

  return (
    <div
      className={`fixed ${
        isPlayerVisible ? 'top-0' : 'top-[102vh]'
      } left-0 h-screen w-full backdrop-blur-sm z-100 p-4 flex items-center justify-center`}
      inert={!isPlayerVisible ? true : undefined}
    >
      <div className="w-full">
        <div className="flex justify-end mb-4">
          <button
            id="yt-view-close-btn"
            aria-label="hide video"
            onClick={() => {
              setIsPlayerVisible(!isPlayerVisible);
            }}
            className="focus:outline-white! focus:outline-2!"
          >
            <MdClose className="w-5 h-5" />
          </button>
        </div>
        <div
          id="player"
          ref={anchorRef}
          className="w-full xl:w-100 h-80 rounded-xl overflow-hidden"
        ></div>
      </div>
    </div>
  );
};

export default YtWrapper;
