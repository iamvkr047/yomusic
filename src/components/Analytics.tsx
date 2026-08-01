import { useEffect } from 'react';

const Analytics = () => {
  useEffect(() => {
    if (import.meta.env.PROD) {
      // Avoid duplicate script appending if component re-renders
      if (document.getElementById('umami-tracker')) return;

      const script = document.createElement('script');
      script.id = 'umami-tracker';
      script.src = 'https://cloud.umami.is/script.js';
      script.defer = true;
      script.dataset.websiteId = import.meta.env.VITE_UMAMI_WEBSITE_ID;

      // Trigger the / hit as soon as the script loads
    //   script.onload = () => {
    //     if ((window as any).umami) {
    //       (window as any).umami.track((props: any) => ({
    //         ...props,
    //         url: '/',
    //         title: 'Homepage',
    //       }));
    //     }
    //   };

      document.body.appendChild(script);
    }
  }, []);

  return null;
};

export default Analytics;
