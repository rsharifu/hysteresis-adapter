package org.surlac.ormlite.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import org.surlac.ormlite.os.AsyncTaskExecutionHelper;

import java.lang.ref.SoftReference;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Comparator;

/**
 * Responsibilities: provide Entity object to create View.
 *
 * @author Ruslan A. Sharifullin
 */
abstract public class AbstractOrmLiteAdapter<T> extends ArrayAdapter<T> {

    public static final String TAG = "# AbstractOrmLiteAdapter";

    protected int mResource;
    protected Class entityClass;
    protected LayoutInflater mInflater;
    protected Context mContext;
    protected OrmLiteSqliteOpenHelper dbHelper;
    protected SparseArray<SoftReference<T>> mObjects;
    private long timesCalled;
    private long accumulatedTime;
    private Dao<T, Integer> dao;

    public AbstractOrmLiteAdapter(OrmLiteSqliteOpenHelper helper, Context context, int resource, Class entity) {
        super(context, resource);
        init(helper, context, resource, entity);
    }

    private void init(OrmLiteSqliteOpenHelper helper, Context context, int resource, Class entity) {
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = resource;
        dbHelper = helper;
        entityClass = entity;
        try {
            dao = dbHelper.getDao(entityClass);
        } catch (SQLException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void initArrayList(int count){
        mObjects = new SparseArray<SoftReference<T>>(count);
    }

    @Override
    abstract public View getView(int position, View convertView, ViewGroup parent);

    abstract public QueryBuilder<T, Integer> applyWhereConditions(QueryBuilder<T, Integer> builder);

    public void eagerFetch(int position, EagerCallback<T> onFetchCompletedCallback, String... fields) {
        AsyncTask asyncTask = new AsyncTask(){
            int position;
            EagerCallback<T> onFetchCompletedCallback;
            String[] fields;
            @Override
            protected Object doInBackground(Object... params) {
                position = (Integer)params[0];
                onFetchCompletedCallback = (EagerCallback<T>) params[1];
                fields = (String[])params[2];
                T entity = null;
                try {
                    entity = applyWhereConditions(dao.queryBuilder())
                            .selectColumns(fields)
                            .limit(1L)
                            .offset((long) position - 1)
                            .queryForFirst();
                } catch (SQLException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
                return entity;
            }

            @Override
            protected void onPostExecute(Object o) {
                onFetchCompletedCallback.onFetchCompleted((T)o);
            }
        };

        AsyncTaskExecutionHelper.executeParallel(asyncTask, position, onFetchCompletedCallback, fields);

    }

    @Override
    @SuppressWarnings("unchecked")
    public T getItem(int position) {
        T object = null;
        if(mObjects == null) {
            initArrayList(getCount());
        }
        if(mObjects.size() > position && mObjects.get(position) != null && mObjects.get(position).get() != null){
            object = mObjects.get(position).get();
        } else {
            try {
                long time = System.nanoTime();
                QueryBuilder<T, Integer> queryBuilder = applyWhereConditions(dao.queryBuilder());
                queryBuilder.limit(1L).offset((long)position - 1);
                object = queryBuilder.queryForFirst();
                mObjects.put(position, new SoftReference<T>(object));
                accumulatedTime += (System.nanoTime() - time);
                timesCalled++;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return object;
    }

    @Override
    public int getCount() {
        try {
            QueryBuilder<T, Integer> queryBuilder = applyWhereConditions(dao.queryBuilder());
            if(queryBuilder == null) {
                return 0;
            }
            int count = (int)queryBuilder.countOf();
            if(mObjects == null) {
                initArrayList(count);
            }
            return count;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return super.getCount();
    }

    @Override
    public int getPosition(T item) {
        return super.getPosition(item);
    }

    /**
     * =================================
     * =====     Not Supported     =====
     * =================================
     */

    @Override
    public void add(T object) {
        throw new UnsupportedOperationException("add()");
    }

    @Override
    public void addAll(Collection collection) {
        throw new UnsupportedOperationException("addAll()");
    }

    @Override
    public void addAll(T... items) {
        throw new UnsupportedOperationException("addAll()");
    }

    @Override
    public void insert(T object, int index) {
        throw new UnsupportedOperationException("insert()");
    }

    @Override
    public void remove(T object) {
        throw new UnsupportedOperationException("remove()");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("clear()");
    }

    @Override
    public void sort(Comparator comparator) {
        throw new UnsupportedOperationException("sort()");
    }

    public double averageCallTimeMs(){
        return (accumulatedTime/timesCalled) / Math.pow(10, 6);
    }

    public interface EagerCallback<E>{
        public void onFetchCompleted(E entity);
    }

}
