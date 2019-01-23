package com.qimeng.jace.dapingji;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@SuppressLint("CheckResult")
public class ImageFragment extends Fragment {

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    Unbinder unbinder;
    private Disposable image;
    private LinearLayoutManager manager;
    private ImageAdapter adapter;


    public static ImageFragment newInstance() {
        ImageFragment fragment = new ImageFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_image, container, false);
        unbinder = ButterKnife.bind(this, root);
        manager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(manager);
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);
        adapter = new ImageAdapter(null, getContext());
        recyclerView.setAdapter(adapter);
        initPic();
        Observable.interval(30, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.newThread())
                .subscribe(l -> initPic());
        return root;
    }

    private void initPic() {//minutes
        HttpUtil.getInstance().getHttp().getGg(MySharedPreferences.getCode())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> {
                    Log.e("Http", data.toString());
                    adapter.putData(data.getPic());
                }, error -> {
                    Observable.timer(5, TimeUnit.SECONDS).subscribeOn(Schedulers.newThread()).subscribe(l -> initPic());
                    error.printStackTrace();
                });
    }


    @Override
    public void onStart() {
        super.onStart();
        image = Observable.interval(4, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.newThread())
                .subscribe(l -> {
                    recyclerView.smoothScrollToPosition(manager.findFirstVisibleItemPosition() + 1);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        if (image != null) {
            image.dispose();
        }
    }
}
