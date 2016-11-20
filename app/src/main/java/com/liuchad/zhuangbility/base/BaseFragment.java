package com.liuchad.zhuangbility.base;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2016/10/15.
 */

public abstract class BaseFragment extends Fragment {

    View mView;

    @Nullable @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(getLayoutId(), null);
        ButterKnife.bind(this, mView);
        return mView;
    }

    @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initInjector();
        initView();
        initData();
    }

    @Override public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override public void onDestroy() {
        super.onDestroy();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected abstract int getLayoutId();

    protected abstract void initView();

    protected abstract void initData();

    protected abstract void initInjector();
}
