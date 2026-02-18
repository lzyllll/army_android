package com.example.demo;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.demo.ui.fragment.AccountFragment;
import com.example.demo.ui.fragment.ApplyFragment;
import com.example.demo.ui.fragment.ArmyFragment;
import com.example.demo.ui.fragment.DonateFragment;
import com.example.demo.ui.fragment.AboutFragment;
import com.example.demo.ui.model.UserAccount;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottomNav);

        // 设置 ViewPager
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return new AccountFragment();
                    case 1:
                        return new ArmyFragment();
                    case 2:
                        return new DonateFragment();
                    case 3:
                        return new AboutFragment();
                    default:
                        return new AccountFragment(); // Keep default as AccountFragment for safety
                }
            }

            @Override
            public int getItemCount() {
                return 4;
            }
        });

        // 禁用 ViewPager 滑动
        viewPager.setUserInputEnabled(false);

        // 底部导航监听
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_account) {
                viewPager.setCurrentItem(0, true);
                return true;
            } else if (itemId == R.id.nav_army) {
                viewPager.setCurrentItem(1, true);
                return true;
            } else if (itemId == R.id.nav_donate) {
                viewPager.setCurrentItem(2, true);
                return true;
            } else if (itemId == R.id.nav_about) {
                viewPager.setCurrentItem(3, true);
                return true;
            }
            return false;
        });

        // ViewPager 页面变化监听
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNav.setSelectedItemId(R.id.nav_account);
                        break;
                    case 1:
                        bottomNav.setSelectedItemId(R.id.nav_army);
                        break;
                    case 2:
                        bottomNav.setSelectedItemId(R.id.nav_donate);
                        break;
                    case 3:
                        bottomNav.setSelectedItemId(R.id.nav_about);
                        break;
                }
            }
        });
    }
}
