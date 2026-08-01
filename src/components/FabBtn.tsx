import { Toolbar, Link } from 'konsta/react';
import { MdEqualizer, MdOutlinePlayArrow, MdOutlinePause } from 'react-icons/md';
import { useLocation, useNavigate } from 'react-router-dom';
import { useDataStore } from '../zustand/app_data_store';

const FabBtn = () => {
  const { currentPlayingVideo, isPlaying, setIsPlaying } = useDataStore();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  return (
    !pathname.includes('/play/') &&
    currentPlayingVideo.isInit && (
      <div className="fixed bottom-[10.5vh] left-0 right-0 z-50 px-4 w-full">
        <Toolbar className=" rounded-xl overflow-hidden w-3/4 mx-auto">
          <div className="flex items-center justify-between w-full px-4 h-full">
            {/* Left: Equalizer Icon */}
            <Link iconOnly className="text-primary">
              <MdEqualizer
                className={`w-6 h-6 text-md-light-primary dark:text-md-dark-primary ${isPlaying && 'animate-pulse'}`}
              />
            </Link>

            {/* Center: Song Title with strict 1-line clamp/ellipsis */}
            <div
              className="flex-1 min-w-0  text-center"
              onClick={() => {
                navigate(`/play/${currentPlayingVideo.videoId}`);
              }}
            >
              <p className="text-sm font-medium line-clamp-1 truncate text-ios-light-text dark:text-ios-dark-text md:text-material-light-text dark:md:text-material-dark-text">
                {currentPlayingVideo.title}
              </p>
            </div>

            {/* Right: Play Icon */}
            <Link
              iconOnly
              className="text-primary"
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                setIsPlaying(!isPlaying);
              }}
            >
              {isPlaying ? (
                <MdOutlinePause className="w-6 h-6 text-md-light-primary dark:text-md-dark-primary" />
              ) : (
                <MdOutlinePlayArrow className="w-6 h-6 text-md-light-primary dark:text-md-dark-primary" />
              )}
            </Link>
          </div>
        </Toolbar>
      </div>
    )
  );
};

export default FabBtn;
