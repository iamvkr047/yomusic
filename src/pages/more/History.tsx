import { ListItem } from 'konsta/react';
import { MdHistory } from 'react-icons/md';
import { useNavigate } from 'react-router-dom';

const HistoryC = () => {
  const navigate = useNavigate();
  return (
    <ListItem
      media={<MdHistory size={24} />}
      onClick={() => {
        navigate('/history');
      }}
      link
      title="History"
      chevronMaterial={false}
      contentClassName="bg-md-light-surface-3 dark:bg-md-dark-surface-3 m-0! rounded-t-lg"
    />
  );
};

export default HistoryC;
