import React, { useState } from 'react';
import Bottombar from '../components/Bottombar';
import { useLocation, useNavigate } from 'react-router-dom';
import { Link, Navbar } from 'konsta/react';
import { MdSearch } from 'react-icons/md';
import DarkModeToggle from '../components/DarkModeToggle';
import { useElementScroll } from '../hooks/useElementScroll';
import { useDataStore } from '../zustand/app_data_store';
import { LuLoaderCircle } from 'react-icons/lu';
import InitCountry from './InitCountry';
import UpdateBlock from '../components/UpdateBlock';
import appLogo from '../assets/app-icon-128px.png';
import InternetChecker from '../components/InternetChecker';

const Layout = ({ children }: { children: React.ReactNode }) => {
  const { scrollRef, handleScroll } = useElementScroll();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const shouldHideBtmBar = pathname.includes('/play/');
  const { isPlayerReady } = useDataStore();
  const [hasCountry, setHasCountry] = useState(true);

  return (
    <>
      <InitCountry hasCountry={hasCountry} setHasCountry={setHasCountry} />
      {hasCountry && (
        <div className="h-full w-full">
          <Navbar
            title="Yo Music"
            className="px-2 w-full flex items-end h-[8vh]"
            left={
              <div
                className="size-7 bg-center bg-contain bg-no-repeat me-2"
                style={{ backgroundImage: `url(${appLogo})` }}
              ></div>
            }
            right={
              <>
                <UpdateBlock />
                {pathname === '/' && (
                  <Link iconOnly onClick={()=>{
                    navigate("/search")
                  }}>
                  <MdSearch className="size-6" />
                </Link>
                )}

                <DarkModeToggle />
              </>
            }
          />
          <div
            ref={scrollRef} // Attach Ref
            onScroll={handleScroll} // Attach Scroll Listener
            className={`px-4 ${
              shouldHideBtmBar ? 'h-[calc(100%_-_8vh)]' : 'h-[calc(100%_-_18vh)]'
            } overflow-y-auto bg-md-light-surface-1 dark:bg-md-dark-surface-1`}
          >
            {children}
          </div>
          {!shouldHideBtmBar && <Bottombar />}

          {/* top loading until player is ready */}
          {!isPlayerReady && (
            <div className="fixed top-0 left-0 h-screen w-full z-500 bg-black/20">
              <div className="w-full h-full flex items-center justify-center ">
                <LuLoaderCircle className="animate-spin size-8" />
              </div>
            </div>
          )}
        </div>
      )}
      <InternetChecker />
    </>
  );
};

export default Layout;
