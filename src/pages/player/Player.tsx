import { LuLoaderCircle } from 'react-icons/lu';
import { useParams } from 'react-router-dom';
import { useDataStore } from '../../zustand/app_data_store';
import MusicControls from './MusicControls';

const Player = () => {
  const params = useParams();
  const { videoId } = params;
  const { currentPlayingVideo } = useDataStore();

  return (
    <div className="dark:text-white max-w-3xl p-4 xl:mx-auto">
      <div
        className="flex w-full justify-center my-4 h-58 rounded-xl"
        style={{
          backgroundSize: 'cover',
          backgroundPosition: 'center',
          backgroundImage: `url('https://i.ytimg.com/vi/${videoId}/hqdefault.jpg')`,
        }}
      ></div>
      {videoId ? (
        <>
          <div className="text-xl xl:text-3xl my-8 line-clamp-2">
            {currentPlayingVideo.title} {currentPlayingVideo.channel}
          </div>
          <div className="text-lg xl:text-xl my-4 text-center">{currentPlayingVideo.channel}</div>
          <MusicControls data={{ ...currentPlayingVideo, videoId: videoId }} />
        </>
      ) : (
        <div className="flex justify-center">
          <LuLoaderCircle className="size-6 animate-spin" />
        </div>
      )}
    </div>
  );
};

export default Player;
