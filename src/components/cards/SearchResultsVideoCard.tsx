import type { VideoType } from '../../types';
import { useDataStore } from '../../zustand/app_data_store';
import { ListItem } from 'konsta/react';
import { playVideo } from '../../playVideo';

const SearchResultsVideoCard = ({ video }: { video: VideoType }) => {
  const { recentSearches, setRecentSearches, setGlobalQueue } = useDataStore();

  const handleClick = () => {
    // add to to recent search:
    // check if this video already exist in recent search to avoid duplicate entry:
    const copy = [...recentSearches];
    const index = copy.findIndex((m) => m.videoId === video.videoId);
    if (index >= 0) {
      // already exist, move it to top:
      copy.splice(index, 1);
      copy.unshift(video);
      setRecentSearches(copy);
    } else {
      // is unique, so add
      const limit = 20;
      setRecentSearches([video, ...copy.slice(0, limit - 1)]);
    }

    // navigate(`/play/${video.videoId}`);
    playVideo(video.videoId);
    //   clear out global queue
    setGlobalQueue({ currentIndex: 0, contents: [] });
  };
  return (
    <div className="" onClick={handleClick}>
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

export default SearchResultsVideoCard;
