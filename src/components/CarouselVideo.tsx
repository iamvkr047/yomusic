import { useCallback, useEffect, useState } from 'react';
import useEmblaCarousel from 'embla-carousel-react';
import { useDataStore } from '../zustand/app_data_store';
import { playVideo } from '../playVideo';
import { Button } from 'konsta/react';

const CarouselVideo = ({
  content,
}: {
  content: {
    videoId: string;
    title: string;
  }[];
}) => {
  const [emblaRef, emblaApi] = useEmblaCarousel({
    dragFree: true,
  });

  const [slidesInView, setSlidesInView] = useState<number[]>([]);

  const updateSlidesInView = useCallback((emblaApii: typeof emblaApi) => {
    if (!emblaApii) {
      return;
    }
    setSlidesInView((slidesInView) => {
      if (slidesInView.length === emblaApii.slideNodes().length) {
        emblaApii.off('slidesInView', updateSlidesInView);
      }
      const inView = emblaApii
        .slidesInView()
        .filter((index: number) => !slidesInView.includes(index));
      return slidesInView.concat(inView);
    });
  }, []);

  useEffect(() => {
    if (!emblaApi) return;

    updateSlidesInView(emblaApi);
    emblaApi.on('slidesInView', updateSlidesInView);
    emblaApi.on('reInit', updateSlidesInView);
    return () => {
      emblaApi.off('slidesInView', updateSlidesInView);
      emblaApi.off('reInit', updateSlidesInView);
    };
  }, [emblaApi, updateSlidesInView]);

  return (
    <div className="embla">
      <div className="embla__viewport overflow-hidden" ref={emblaRef}>
        <div className="embla__container flex touch-pan-y touch-pinch-zoom">
          {content.map((m, ind) => (
            <LazyLoadImage
              key={ind}
              index={ind}
              videoId={m.videoId}
              inView={slidesInView.indexOf(ind) > -1}
              content={content}
            />
          ))}
        </div>
      </div>
    </div>
  );
};

export default CarouselVideo;

const LazyLoadImage = (props: {
  videoId: string;
  inView: boolean;
  index: number;
  content: {
    videoId: string;
    title: string;
  }[];
}) => {
  const { videoId, inView, index, content } = props;
  const { setGlobalQueue } = useDataStore();
  const [hasLoaded, setHasLoaded] = useState(false);

  useEffect(() => {
    if (inView) {
      setHasLoaded(true);
    }
  }, [inView]);

  return (
    <div key={videoId} className="shrink-0 px-2 basis-[60%] grow-1 min-w-0">
      <Button
        className="h-40 bg-slate-300 rounded-2xl"
        style={{
          backgroundImage: hasLoaded
            ? `url('https://i.ytimg.com/vi/${videoId}/mqdefault.jpg')`
            : '',
          backgroundRepeat: 'no-repeat',
          backgroundSize: 'cover',
          backgroundPosition: 'center',
        }}
        onClick={() => {
        //   navigate(`/play/${videoId}`);
        playVideo(videoId);

          setGlobalQueue({
            currentIndex: index,
            contents: content.map((item) => ({
              videoId: item.videoId,
              title: item.title,
              channel: '',
              duration: 0,
            })),
          });
        }}
      ></Button>
    </div>
  );
};
