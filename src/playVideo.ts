import { Yt } from './api';
import type { VideoType } from './types';
import { useDataStore } from './zustand/app_data_store';

// The function responsible to play the video:
export const playVideo = (videoId: string) => {
  // check if supplied same videoId as current Playing:
  if (!videoId) {
    return;
  }
  const { currentPlayingVideo, setcurrentPlayingVideo, isPlaying, setIsPlaying } =
    useDataStore.getState();
  if (videoId === currentPlayingVideo.videoId) {
    // same video req to play
    if (!isPlaying) {
      setIsPlaying(true);
    }
    return;
  }
  // requested a new video
  const cachedVideoMap = localStorage.getItem('cachedVideoMap');
  if (cachedVideoMap) {
    // use cache
    //   check if video exist in cache:
    const parsedCachedVideoMap = JSON.parse(cachedVideoMap);
    if (videoId in parsedCachedVideoMap) {
      // has cache:
      setcurrentPlayingVideo({
        ...parsedCachedVideoMap[videoId],
        isInit: true,
      });
      setIsPlaying(true);
      addToHistory(parsedCachedVideoMap[videoId]);
    } else {
      fetchVideoDetails(videoId);
    }
  } else {
    fetchVideoDetails(videoId);
  }
};

const fetchVideoDetails = async (videoId: string) => {
  const { setcurrentPlayingVideo, setIsPlaying } = useDataStore.getState();
  try {
    const videoData = await Yt.getVideoData(videoId);
    if (!videoData) {
      return false;
    }
    setcurrentPlayingVideo({ ...videoData, isInit: true });
    setIsPlaying(true);

    // add this video to LOCALSTORAGE cachedVideoMap:
    let parsedCachedVideoMap: { [x: string]: object } = {};
    const cachedVideoMap = localStorage.getItem('cachedVideoMap');
    if (cachedVideoMap) {
      parsedCachedVideoMap = JSON.parse(cachedVideoMap);
    }
    parsedCachedVideoMap[videoId!] = videoData;
    localStorage.setItem(`cachedVideoMap`, JSON.stringify(parsedCachedVideoMap));

    //   add to history
    addToHistory(videoData);
  } catch (error) {}
};

const addToHistory = (video: VideoType) => {
  const { history, setHistory } = useDataStore.getState();
  // check if already exist:
  const v = history.some((f) => f.videoId === video.videoId);
  if (v) {
    // has already, just move to top
    setHistory([video, ...history.filter((f) => f.videoId !== video.videoId)]);
  } else {
    // add to top of list with limit:
    const limit = 30;
    setHistory([video, ...history.slice(0, limit - 1)]);
  }
};
