import type { VideoType } from '../../types';
import { MdClose } from 'react-icons/md';
import { useDataStore } from '../../zustand/app_data_store';
import { ListItem } from 'konsta/react';
import { playVideo } from '../../playVideo';

const RecentSearchesCard = ({ data }: { data: VideoType }) => {
  const { videoId, title, channel } = data;
  const { recentSearches, setRecentSearches, setGlobalQueue } = useDataStore();
  return (
    <div
      className="rounded-lg"
      onClick={() => {
        // navigate(`/play/${videoId}`);
        playVideo(videoId);
        // get  index
        const i = recentSearches.findIndex((item) => item.videoId === videoId);
        setGlobalQueue({
          currentIndex: i,
          contents: recentSearches,
        });
      }}
    >
      <ListItem
        contentClassName="bg-md-light-surface-3 dark:bg-md-dark-surface-3 m-0! my-3! rounded-lg"
        titleWrapClassName="title_clamp_style" // inject style for title line clamp
        link
        chevronMaterial={false}
        title={title + 'lorem test tsest test tsest'}
        after={
          <MdClose
            className="size-6"
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              // delete this item from recent searches:
              setRecentSearches(recentSearches.filter((sr) => sr.videoId != videoId));
            }}
          />
        }
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

      <style>
        {`
        .title_clamp_style > div:first-child {
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
        overflow: hidden;
        }
        `}
      </style>
    </div>
  );
};

export default RecentSearchesCard;
