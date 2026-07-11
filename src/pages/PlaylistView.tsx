import { useEffect, useMemo, useState } from 'react';
import { Yt } from '../api';
import { Button, List } from 'konsta/react';
import VideoCard from '../components/cards/VideoCard';
import { LuLoaderCircle } from 'react-icons/lu';
import { useNavigate, useParams } from 'react-router-dom';
import type { PlaylistType, VideoType } from '../types';
import { MdFavorite, MdFavoriteBorder, MdPlayArrow, MdPlaylistPlay, MdShare } from 'react-icons/md';
import { useDataStore } from '../zustand/app_data_store';
import { handleShare } from '../utils/shareText';
import { toastMsg } from '../utils/toastMsg';
import { compressImage } from '../utils/getBase64FromUrl';

const PlaylistView = () => {
  const { playlistId } = useParams();
  const { playlistData, setplaylistData, playlists, setPlaylists } = useDataStore();
  const [loading, setloading] = useState(false);
  const [loadingNext, setloadingNext] = useState(false);
  const navigate = useNavigate();

  const getData = async (playlistId: string) => {
    setloading(true);
    const res = await Yt.getPlaylistVideos(playlistId);

    if (!res) {
      return false;
    }

    const { playlist, context, apiToken } = res;
    // save context and api token to local storage for next page:
    localStorage.setItem('pageClient', JSON.stringify({ context, apiToken }));
    const formattedObj = {
      playlistId,
      title: playlist.title,
      thumbnail: playlist.thumbnail,
      isCustomChannel: false,
      channel: playlist.channel || '',
      videoItems: {
        items: playlist.videoItems.items as VideoType[],
        nextPage: playlist.videoItems.nextPage,
      },
      total: playlist.total,
    };

    setplaylistData(formattedObj);
    setloading(false);
    // save to cache
    saveToCache(formattedObj, playlistId);
  };

  const getNext = async () => {
    const token = playlistData.videoItems.nextPage;
    if (!token) {
      return false;
    }
    // get apiToken and context from localstorage:
    const next = localStorage.getItem('pageClient');
    if (!next) {
      return false;
    }
    const { context, apiToken } = JSON.parse(next);

    setloadingNext(true);
    const res = await Yt.getPlaylistVideosNext(token, context, apiToken);
    if (!res) {
      return false;
    }
    const updatedVideos = [...playlistData.videoItems.items, ...res.items] as VideoType[];
    const formattedObj = {
      ...playlistData,
      videoItems: { items: updatedVideos, nextPage: res.nextPage },
    };
    setplaylistData(formattedObj);

    // save to cache
    setloadingNext(false);
    saveToCache(formattedObj, playlistData.playlistId);
  };

  useEffect(() => {
    if (playlistId && playlistId !== playlistData.playlistId) {
      // check cached
      const cachedPlaylistMap = sessionStorage.getItem('cachedPlaylistMap');
      if (cachedPlaylistMap) {
        // use cache
        // check if playlist exist in cache:
        const parsedCachedPlaylistMap = JSON.parse(cachedPlaylistMap);
        if (playlistId in parsedCachedPlaylistMap) {
          // has cache:
          setplaylistData(parsedCachedPlaylistMap[playlistId]);
        } else {
          getData(playlistId);
        }
      } else {
        getData(playlistId);
      }
    }
  }, []);

  function saveToCache(formattedObj: PlaylistType, playlistId: string) {
    let parsedCachedPlaylistMap: { [x: string]: object } = {};
    const cachedPlaylistMap = sessionStorage.getItem('cachedPlaylistMap');
    if (cachedPlaylistMap) {
      parsedCachedPlaylistMap = JSON.parse(cachedPlaylistMap);
    }
    parsedCachedPlaylistMap[playlistId] = formattedObj;
    sessionStorage.setItem(`cachedPlaylistMap`, JSON.stringify(parsedCachedPlaylistMap));
  }

  const isAlreadySavedPl = useMemo(
    () => playlists.some((item) => item.playlistId === playlistId),
    [playlists, playlistId]
  );

  const toggleSaveToPl = async () => {
    if (isAlreadySavedPl) {
      setPlaylists(playlists.filter((f) => f.playlistId != playlistId));
      toastMsg('Removed from playlists');
    } else {
      if (playlistData.playlistId) {
        const formattedObj = {
          ...playlistData,
          isCustomChannel: false,
          videoItems: { items: [], nextPage: '' },
        };
        // handle RD playlists thumbnail as it keeps changing so need to convert to data:base64 and store it:
        if (playlistData.playlistId.startsWith('RD')) {
          const compressedBase64Url = await compressImage(playlistData.thumbnail);
          formattedObj.thumbnail = compressedBase64Url;
        }

        setPlaylists([formattedObj, ...playlists]);
        toastMsg('Saved to playlists');
      }
    }
  };

  return (
    <div className="w-full">
      {loading ? (
        <div className="my-4 flex justify-center">
          <LuLoaderCircle className="size-6 animate-spin" />
        </div>
      ) : (
        <div>
          {playlistData && (
            <div className="py-2">
              <div className="flex gap-4 w-full my-4 items-center">
                <div>
                  {playlistData.thumbnail && (
                    <div
                      style={{
                        backgroundImage: `url('${playlistData.thumbnail}')`,
                        backgroundSize: 'cover',
                        backgroundPosition: 'center',
                      }}
                      className="w-20 h-20 ms-2 rounded-xl overflow-hidden"
                    >
                      <MdPlaylistPlay size={24} className="bg-black text-white" />
                    </div>
                  )}
                </div>
                <div className="flex-1">
                  <p className="text-xl font-semibold line-clamp-2">{playlistData.title}</p>
                  <div className="my-1 items-center text-sm">
                    <span className="">{playlistData.channel || 'playlist'}</span>
                    {playlistData.total && (
                      <>
                        {/* <span className=" font-bold">&#8226;</span> */}
                        <p>{playlistData.total} Videos</p>
                      </>
                    )}
                  </div>
                  <div className="flex gap-2 justify-between items-center w-full">
                    <Button
                      className="w-full"
                      rounded={true}
                      onClick={() => {
                        navigate(`/play/${playlistData.videoItems.items[0].videoId}`);
                      }}
                    >
                      <MdPlayArrow size={18} className="me-2" />
                      Play
                    </Button>
                    <span onClick={toggleSaveToPl}>
                      {isAlreadySavedPl ? <MdFavorite size={24} /> : <MdFavoriteBorder size={24} />}
                    </span>
                    <span
                      onClick={() => {
                        handleShare({
                          title: 'Watch this playlist at',
                          url: `https://www.youtube.com/playlist?list=${playlistData.playlistId}`,
                        });
                      }}
                    >
                      <MdShare size={24} />
                    </span>
                  </div>
                </div>
              </div>
            </div>
          )}
          <div className="">
            {playlistData.videoItems.items && (
              <List>
                {playlistData.videoItems.items.map((video, i) => (
                  <VideoCard
                    video={video}
                    key={video.videoId + String(i)}
                    playlistData={playlistData}
                  />
                ))}

                {/* next page btn */}
                {playlistData.videoItems.nextPage && (
                  <div className="flex w-full pt-4">
                    <Button disabled={loadingNext} rounded className="shrink-0" onClick={getNext}>
                      Load More
                    </Button>
                  </div>
                )}
              </List>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default PlaylistView;
