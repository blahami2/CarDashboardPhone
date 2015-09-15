package com.eclubprague.cardashboard.phone.fragments;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eclubprague.cardashboard.core.application.GlobalDataProvider;
import com.eclubprague.cardashboard.core.data.database.ModuleDAO;
import com.eclubprague.cardashboard.core.modules.base.IModule;
import com.eclubprague.cardashboard.core.modules.base.IParentModule;
import com.eclubprague.cardashboard.phone.R;
import com.eclubprague.cardashboard.phone.activities.DnDActivity;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class DnDFragment extends ListFragment {

    public static String TAG = DnDFragment.class.getSimpleName();
    private ArrayAdapter<IModule> mAdapter;
    private IParentModule mParentModule;

    public static final String PARENT_MODULES_SCOPE_ID = "parentModulesScopeId";
    private static ArrayList<IParentModule> parentModulesScope = new ArrayList<>();
    private static int parentModulesScopeId = 0;

    private final DragSortListView.DropListener mDropListener =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    if (from != to) {
                        IModule item = mAdapter.getItem(from);
                        mAdapter.remove(item);
                        mAdapter.insert(item, to);
                        try {
                            ModuleDAO.saveParentModuleAsync(GlobalDataProvider.getInstance().getContext(), mParentModule);
                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                }
            };

    private final DragSortListView.RemoveListener mRemoveListener =
            new DragSortListView.RemoveListener() {
                @Override
                public void remove(int which) {
                    mAdapter.remove(mAdapter.getItem(which));
                }
            };

    private DragSortListView mDslv;
    private DragSortController mController;

    public int dragStartMode = DragSortController.ON_DOWN;
    public boolean removeEnabled = false;
    public int removeMode = DragSortController.FLING_REMOVE;
    public boolean sortEnabled = true;
    public boolean dragEnabled = true;


    public DnDFragment() {
        super();
    }

    public DragSortController getController() {
        return mController;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDslv = (DragSortListView) inflater.inflate(R.layout.dslv_fragment_main, container, false);

        // defaults are
        //   dragStartMode = onDown
        //   removeMode = flingRight
        DragSortController controller = new DragSortController(mDslv);
        controller.setDragHandleId(R.id.drag_handle);
        controller.setClickRemoveId(R.id.click_remove);
        controller.setRemoveEnabled(removeEnabled);
        controller.setSortEnabled(sortEnabled);
        controller.setDragInitMode(dragStartMode);
        controller.setRemoveMode(removeMode);

        mController = controller;
        mDslv.setFloatViewManager(mController);
        mDslv.setOnTouchListener(mController);
        mDslv.setDragEnabled(dragEnabled);

        return mDslv;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mDslv = (DragSortListView) getListView();

        mDslv.setDropListener(mDropListener);
        mDslv.setRemoveListener(mRemoveListener);

        int parentScopeId = this.getActivity().getIntent().getIntExtra(DnDFragment.PARENT_MODULES_SCOPE_ID, -1);
        if (parentScopeId == -1) {
            try {
                mParentModule = ModuleDAO.loadParentModule(GlobalDataProvider.getInstance().getContext());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            mParentModule = parentModulesScope.get(parentScopeId);
        }
        if (mParentModule == null) return;


        //mAdapter = new ArrayAdapter<>(getActivity(), R.layout.list_item_handle_left, R.id.text, mParentModule.getSubmodules());
        mAdapter = new IModuleArrayAdapter(getActivity(), R.id.text, mParentModule.getSubmodules());
        setListAdapter(mAdapter);
        ListView lv = getListView();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int clickedItemNumber,
                                    long arg3) {
                IModule current = mAdapter.getItem(clickedItemNumber);
                if (current instanceof IParentModule) {

                    parentModulesScope.add((IParentModule) current);
                    parentModulesScopeId++;
                    Intent intent = new Intent(getActivity(), DnDActivity.class);
                    intent.putExtra(DnDFragment.PARENT_MODULES_SCOPE_ID, parentModulesScopeId - 1);
                    startActivity(intent);
                }
            }
        });
//        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
//                                           long arg3) {
//                String message = String.format("Long-clicked item %d", arg2);
//                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
//                return true;
//            }
//        });
    }




    private class IModuleArrayAdapter extends ArrayAdapter<IModule> {

        private class ViewHolder {
            private TextView itemView;
        }

        public IModuleArrayAdapter(Context context, int textViewResourceId, List<IModule> items) {
            super(context, textViewResourceId, items);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(this.getContext())
                        .inflate(R.layout.list_item_handle_left, parent, false);

                viewHolder = new ViewHolder();
                viewHolder.itemView = (TextView) convertView.findViewById(R.id.text);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            IModule item = getItem(position);
            if (item != null) {
                // My layout has only one TextView
                // do whatever you want with your string and long
                viewHolder.itemView.setText(item.getTitle().getString());
            }

            return convertView;
        }

    }
}
