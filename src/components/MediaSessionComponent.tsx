import { useEffect } from 'react';
import { CapacitorMusicControls } from 'capacitor-music-controls-plugin';
import { useDataStore } from '../zustand/app_data_store';
import { playVideo } from '../playVideo';

const MediaSessionComponent = () => {
  const { globalQueue, setGlobalQueue, currentPlayingVideo, isPlaying, setIsPlaying } =
    useDataStore();

  useEffect(() => {
    if (currentPlayingVideo.videoId && currentPlayingVideo.videoId !== 'M7lc1UVf-VE') {
      //ignore the fallback video
      CapacitorMusicControls.create({
        track: currentPlayingVideo.title, // optional, default : ''
        artist: currentPlayingVideo.channel, // optional, default : ''
        // album: "Absolution", // optional, default: ''
        cover: `https://i.ytimg.com/vi/${currentPlayingVideo.videoId}/mqdefault.jpg`, // optional, default : nothing
        // cover can be a local path (use fullpath 'file:///storage/emulated/...',
        // or only 'my_image.jpg' if my_image.jpg is in the www folder of your app)
        // or a remote url ('http://...', 'https://...', 'ftp://...')

        // hide previous/next/close buttons:
        hasPrev: true, // show previous button, optional, default: true
        hasNext: true, // show next button, optional, default: true
        hasClose: true, // show close button, optional, default: false

        // iOS only, all optional
        // duration: 60, // default: 0
        // elapsed: 10, // default: 0
        // hasSkipForward: true, // default: false. true value overrides hasNext.
        // hasSkipBackward: true, // default: false. true value overrides hasPrev.
        // skipForwardInterval: 15, // default: 15.
        // skipBackwardInterval: 15, // default: 15.
        // hasScrubbing: false, // default: false. Enable scrubbing from control center progress bar

        // Android only, all optional
        isPlaying: isPlaying, // default : true
        dismissable: true, // default : false
        // text displayed in the status bar when the notification (and the ticker) are updated
        ticker: currentPlayingVideo.title,
        // All icons default to their built-in android equivalents
        // The supplied drawable name, e.g. 'media_play', is the name of a drawable found under android/res/drawable* folders
        playIcon: 'ic_play',
        pauseIcon: 'ic_pause',
        prevIcon: 'ic_chev_left',
        nextIcon: 'ic_chev_right',
        closeIcon: 'ic_close',
        notificationIcon: 'graphic_eq',
      })
        .then(() => {
          // SUCCESS
        })
        .catch(() => {});

      const eventer = (event: Event) => {
        const info = { message: (event as any).message, position: 0 };
        handleControlsEvent(info);
      };
      document.addEventListener('controlsNotification', eventer);
      return () => {
        document.removeEventListener('controlsNotification', eventer);
        // CapacitorMusicControls.destroy();
      };
    }
  }, [currentPlayingVideo]);

  function PlayPrev() {
    if (globalQueue.contents.length <= 0 || globalQueue.currentIndex <= 0) {
      return false;
    }
    // navigate('/play/' + globalQueue.contents[globalQueue.currentIndex - 1].videoId, {
    //   replace: window.location.href.includes('/play/') ? true : false,
    // });
    playVideo(globalQueue.contents[globalQueue.currentIndex - 1].videoId);
    // update global queue too:
    setGlobalQueue({
      ...globalQueue,
      currentIndex: globalQueue.currentIndex - 1,
    });
  }

  function PlayNext() {
    if (
      globalQueue.contents.length <= 0 ||
      globalQueue.currentIndex >= globalQueue.contents.length - 1
    ) {
      return false;
    }
    // navigate('/play/' + globalQueue.contents[globalQueue.currentIndex + 1].videoId, {
    //   replace: window.location.href.includes('/play/') ? true : false,
    // });
    playVideo(globalQueue.contents[globalQueue.currentIndex + 1].videoId);
    // update global queue too:
    setGlobalQueue({
      ...globalQueue,
      currentIndex: globalQueue.currentIndex + 1,
    });
  }

  function handleControlsEvent(action: { message: any; position: number }) {
    const message = action.message;
    switch (message) {
      case 'music-controls-next':
        // next
        PlayNext();
        break;
      case 'music-controls-previous':
        // previous
        PlayPrev();
        break;
      case 'music-controls-pause':
        // paused
        setIsPlaying(false);
        break;
      case 'music-controls-play':
        // resumed
        setIsPlaying(true);
        break;
      case 'music-controls-destroy':
        // controls were destroyed
        break;
    }
  }

  useEffect(() => {
    CapacitorMusicControls.updateIsPlaying({ isPlaying: isPlaying });
  }, [isPlaying]);

  return <></>;
};

export default MediaSessionComponent;
