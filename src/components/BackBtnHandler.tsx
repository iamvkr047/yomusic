import { useEffect } from 'react';
import { App } from '@capacitor/app';
import { useDataStore } from '../zustand/app_data_store';
import { useLocation, useNavigate } from 'react-router-dom';
import { playVideo } from '../playVideo';

const BackBtnHandler = () => {
  const { globalDialogData, setglobalDialogData, isPlayerVisible, setIsPlayerVisible } =
    useDataStore();
  const closeDialog = () => {
    setglobalDialogData({ ...globalDialogData, isOpen: false });
  };
  const navigate = useNavigate();
  const { pathname } = useLocation();
  useEffect(() => {
    App.addListener('backButton', ({ canGoBack }) => {
      if (isPlayerVisible) {
        setIsPlayerVisible(false); // close down video player dialog if opened
      } else if (globalDialogData.isOpen) {
        closeDialog();
      } else if (canGoBack) {
        window.history.back();
      } else {
        // showExitDialog();
        App.minimizeApp();
      }

      /** OPEN VIA URL HANDLE **/
      App.addListener('appUrlOpen', (event) => {
        try {
          const parsedUrl = new URL(event.url);
          // 1. Check for Playlist ID (usually 'list=' parameter)
          const playlistId = parsedUrl.searchParams.get('list');

          // 2. Check for Video ID ('v=' parameter or path in youtu.be)
          let videoId =
            parsedUrl.searchParams.get('v') || parsedUrl.hostname === 'youtu.be'
              ? parsedUrl.pathname.slice(1)
              : null;

          // Logic for navigation
          if (playlistId) {
            // If it's a playlist, go to the list route
            navigate(`/list/${playlistId}`);
          } else if (videoId) {
            // If it's just a video, go to the play route
            // navigate(`/play/${videoId}`);
            playVideo(videoId);
          } else {
            alert('Invalid YouTube Video or Playlist URL');
          }
        } catch (error) {
          alert('Invalid URL YouTube link.');
        }
      });
    });

    return () => {
      App.removeAllListeners();
    };
  }, [pathname, globalDialogData.isOpen, isPlayerVisible]);

  return <div></div>;
};

export default BackBtnHandler;

// const showExitDialog = () => {
//   setglobalDialogData({
//     isOpen: true,
//     title: "Exit",
//     subtitle: "Are you sure to exit?",
//     ok: {
//       okText: "Ok",
//       onOk: () => {
//         App.exitApp();
//       },
//     },
//     cancel: {
//       cancelText: "Cancel",
//     },
//   });
// };
