package org.tamal.mobileinfo;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ViewPager.OnPageChangeListener {

    private Menu menu;
    private ViewPager viewPager;
    private AbstractFragment[] fragments = {
            new HomeFragment(),
            new FontsFragment(),
            new NetworkFragment(),
            new SensorsFragment(),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        viewPager = findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.addOnPageChangeListener(this);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        menu = navigationView.getMenu();
        for (AbstractFragment fragment : fragments) {
            MenuItem menuItem = menu.add(R.id.menu_group, fragment.hashCode(), Menu.NONE, fragment.getTitle());
            menuItem.setIcon(fragment.getIcon());
            menuItem.setCheckable(true);
        }
        menu.getItem(0).setChecked(true);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        for (int i = 0; i < fragments.length; i++) {
            if (item.getItemId() == fragments[i].hashCode()) {
                viewPager.setCurrentItem(i);
                break;
            }
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // Empty
    }

    @Override
    public void onPageSelected(int position) {
        menu.getItem(position).setChecked(true);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // Empty
    }

    class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(fragments[position].getTitle());
        }

        @Override
        public int getCount() {
            return fragments.length;
        }
    }
}
