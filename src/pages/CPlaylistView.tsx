import { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useDataStore } from '../zustand/app_data_store';
import { MdPlayArrow, MdPlaylistPlay } from 'react-icons/md';
import { Button, List } from 'konsta/react';
import VideoCard from '../components/cards/VideoCard';
import { playVideo } from '../playVideo';

const CPlaylistView = () => {
  const { playlistId } = useParams();
  const { playlists, playlistData, setplaylistData } = useDataStore();

  useEffect(() => {
    if (playlistId && playlistId !== playlistData.playlistId) {
      const obj = playlists.find((p) => p.playlistId === playlistId);
      if (obj) {
        setplaylistData(obj);
      }
    }
  }, []);

  return (
    <div className="w-full">
      <div className="flex gap-4 w-full my-4 items-center">
        <div className="bg-md-light-primary dark:bg-md-dark-primary text-on-primary ms-2 rounded-xl">
          {
            <div className="w-20 h-20  flex items-center justify-center">
              <MdPlaylistPlay
                size={32}
                className="text-md-light-on-primary dark:text-md-dark-on-primary"
              />
            </div>
          }
        </div>
        <div className="flex-1">
          <p className="text-xl font-semibold line-clamp-2">{playlistData.title}</p>
          <div className="flex gap-2 my-1 items-center">
            <span>Custom Playlist</span>
            <span className=" font-bold">&#8226;</span>
            <p>{playlistData.videoItems.items.length} Videos</p>
          </div>
          <div className="flex gap-2 justify-between items-center w-full">
            <Button
              rounded={true}
              className="w-full"
              onClick={() => {
                // navigate(`/play/${}`);
                playVideo(playlistData.videoItems.items[0].videoId);
              }}
            >
              <MdPlayArrow size={18} className="me-2" />
              Play
            </Button>
          </div>
        </div>
      </div>

      {playlistData.videoItems.items.length > 0 && (
        <List>
          {playlistData.videoItems.items.map((item, i) => (
            <VideoCard video={item} key={i} playlistData={playlistData} />
          ))}
        </List>
      )}
    </div>
  );
};

export default CPlaylistView;
