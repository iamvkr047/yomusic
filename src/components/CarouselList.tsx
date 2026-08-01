import { useCallback, useEffect, useState } from 'react';
import useEmblaCarousel from 'embla-carousel-react';
import { useNavigate } from 'react-router-dom';
import { Button } from 'konsta/react';

const CarouselList = ({
  content,
}: {
  content: {
    playlistId: string;
    title: string;
    thumbnail: string;
  }[];
}) => {
  const [emblaRef, emblaApi] = useEmblaCarousel({
    dragFree: true,
  });

  const [slidesInView, setSlidesInView] = useState<Set<string>>(new Set([]));

  const updateSlidesInView = useCallback((emblaApii: typeof emblaApi) => {
    if (!emblaApii) {
      return;
    }
    const visibleSlides = emblaApii.slidesInView().map((i) => content[i].playlistId);
    setSlidesInView(new Set([...visibleSlides, ...Array.from(slidesInView)]));
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
              playlistId={m.playlistId}
              thumbnail={m.thumbnail}
              inView={slidesInView.has(m.playlistId)}
              title={m.title}
            />
          ))}
        </div>
      </div>
    </div>
  );
};

export default CarouselList;

const LazyLoadImage = (props: {
  playlistId: string;
  thumbnail: string;
  inView: boolean;
  index: number;
  title: string;
}) => {
  const { playlistId, thumbnail, inView } = props;
  const navigate = useNavigate();
  const [hasLoaded, setHasLoaded] = useState(false);

  useEffect(() => {
    if (inView) {
      setHasLoaded(true);
    }
  }, [inView]);

  return (
    <div key={playlistId} className="shrink-0 px-2 basis-[60%] grow-1 min-w-0">
      <Button
        className="h-40 bg-slate-300 rounded-2xl"
        style={{
          backgroundImage: hasLoaded ? `url('${thumbnail}')` : '',
          backgroundRepeat: 'no-repeat',
          backgroundSize: 'cover',
          backgroundPosition: 'center',
        }}
        onClick={() => {
          navigate(`/list/${playlistId}`);
        }}
      ></Button>
    </div>
  );
};
