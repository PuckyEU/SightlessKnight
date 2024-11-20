import {SideNav} from '@hilla/react-components/SideNav';
import {SideNavItem} from '@hilla/react-components/SideNavItem';
import {AppLayout} from '@hilla/react-components/AppLayout';
import {DrawerToggle} from '@hilla/react-components/DrawerToggle';
import styles from 'Frontend/themes/sightlessknight/main-layout.module.css';

export default function ApplicationLayout({children} : {children: any}) {

    return (
        <>
            <AppLayout className={styles.parent}>
                <DrawerToggle slot="navbar" />
                <h3 slot="navbar">
                    <a href="/">SightlessKnight</a>
                </h3>

                <SideNav slot="drawer" className={styles.aa}>
                    <SideNavItem path="/">
                        Home
                    </SideNavItem>
                    <SideNavItem path="/play">
                        Play
                    </SideNavItem>
                    <SideNavItem path="/train">
                        Train
                    </SideNavItem>
                </SideNav>

                {children}
            </AppLayout>
        </>
    )
}