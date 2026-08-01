import ImportExport from '../more/ImportExport';
import Region from '../more/Region';
import ShareP from '../more/ShareP';
import About from '../more/About';
import Updates from '../more/Updates';
import PirvacyPolicy from '../more/PirvacyPolicy';
import HistoryC from '../more/History';
import { List } from 'konsta/react';
import { Fragment } from 'react/jsx-runtime';
// import SleepTimer from "../more/SleepTimer";

const More = () => {
  const options = [
    {
      title: 'History',
      component: <HistoryC />,
    },
    {
      title: 'Your Data',
      component: <ImportExport />,
    },
    {
      title: 'Region',
      component: <Region />,
    },
    {
      title: 'Share',
      component: <ShareP />,
    },
    {
      title: 'Updates',
      component: <Updates />,
    },
    // {
    //   title: "SleepTimer",
    //   component: <SleepTimer />,
    // },
    {
      title: 'About',
      component: <About />,
    },
    {
      title: 'PrivacyPolicy',
      component: <PirvacyPolicy />,
    },
  ];

  return (
    <div>
      <div className="title font-semibold text-lg my-4">More</div>
      {/* settings list */}
      <div>
        <List>
          {options.map((item) => (
            <Fragment key={item.title}>{item.component}</Fragment>
          ))}
        </List>
      </div>
    </div>
  );
};

export default More;
