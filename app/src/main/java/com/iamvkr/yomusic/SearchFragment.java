package com.iamvkr.yomusic;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.iamvkr.yomusic.adapters.SearchSuggestionAdapter;
import com.iamvkr.yomusic.api.YtRepository;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private static final String PREFS_NAME = "YoMusicPrefs";
    private static final String KEY_RECENT_SEARCHES = "recentSearches";
    private static final int MAX_RECENT = 10;
    private static final long DEBOUNCE_MS = 600;

    private TextInputEditText etSearchQuery;
    private LinearProgressIndicator searchProgress;
    private RecyclerView rvSuggestions;
    private View tvSuggestionsLabel;
    private RecyclerView rvRecentSearches;
    private View tvRecentLabel;

    private SearchSuggestionAdapter suggestionAdapter;
    private SearchSuggestionAdapter recentAdapter;
    private final List<String> suggestions = new ArrayList<>();
    private final List<String> recentSearches = new ArrayList<>();

    private YtRepository repository;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearchQuery = view.findViewById(R.id.et_search_query);
        searchProgress = view.findViewById(R.id.search_progress);
        rvSuggestions = view.findViewById(R.id.rv_suggestions);
        tvSuggestionsLabel = view.findViewById(R.id.tv_suggestions_label);
        rvRecentSearches = view.findViewById(R.id.rv_recent_searches);
        tvRecentLabel = view.findViewById(R.id.layout_recent_header); // Using the header layout instead of just text
        
        View btnClearRecent = view.findViewById(R.id.btn_clear_recent);
        if (btnClearRecent != null) {
            btnClearRecent.setOnClickListener(v -> clearRecentSearches());
        }

        repository = new YtRepository(requireContext());

        // Suggestions adapter
        suggestionAdapter = new SearchSuggestionAdapter(suggestions, new SearchSuggestionAdapter.OnSuggestionClickListener() {
            @Override
            public void onSuggestionClick(String suggestion) {
                performSearch(suggestion);
            }

            @Override
            public void onSuggestionCopyToInput(String suggestion) {
                etSearchQuery.setText(suggestion);
                etSearchQuery.setSelection(suggestion.length());
            }
        });
        rvSuggestions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSuggestions.setAdapter(suggestionAdapter);

        // Recent searches adapter - clicking a recent search triggers actual search
        recentAdapter = new SearchSuggestionAdapter(recentSearches, new SearchSuggestionAdapter.OnSuggestionClickListener() {
            @Override
            public void onSuggestionClick(String suggestion) {
                performSearch(suggestion);
            }

            @Override
            public void onSuggestionCopyToInput(String suggestion) {
                etSearchQuery.setText(suggestion);
                etSearchQuery.setSelection(suggestion.length());
            }
        });
        rvRecentSearches.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentSearches.setAdapter(recentAdapter);

        loadRecentSearches();
        showRecentIfAvailable();

        // Debounced autocomplete
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    hideSuggestions();
                    showRecentIfAvailable();
                    return;
                }
                hideRecent();
                debounceRunnable = () -> fetchSuggestions(query);
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
            }
        });

        // Pressing Enter/Search on the keyboard
        etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String q = etSearchQuery.getText() != null ? etSearchQuery.getText().toString().trim() : "";
                if (!q.isEmpty()) performSearch(q);
                return true;
            }
            return false;
        });

        // Focus & open keyboard
        etSearchQuery.requestFocus();
        etSearchQuery.post(() -> {
            InputMethodManager imm = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(etSearchQuery, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private void fetchSuggestions(String query) {
        searchProgress.setVisibility(View.VISIBLE);
        repository.getAutoCompleteSearch(query, new YtRepository.OnAutoCompleteCallback() {
            @Override
            public void onSuccess(List<String> result) {
                if (!isAdded()) return;
                searchProgress.setVisibility(View.GONE);
                suggestions.clear();
                suggestions.addAll(result);
                suggestionAdapter.notifyDataSetChanged();
                if (!suggestions.isEmpty()) {
                    rvSuggestions.setVisibility(View.VISIBLE);
                    tvSuggestionsLabel.setVisibility(View.VISIBLE);
                } else {
                    hideSuggestions();
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                searchProgress.setVisibility(View.GONE);
            }
        });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) return;
        dismissKeyboard();
        saveToRecentSearches(query);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openSearchResults(query);
        }
    }

    private void hideSuggestions() {
        suggestions.clear();
        suggestionAdapter.notifyDataSetChanged();
        rvSuggestions.setVisibility(View.GONE);
        tvSuggestionsLabel.setVisibility(View.GONE);
    }

    private void hideRecent() {
        rvRecentSearches.setVisibility(View.GONE);
        tvRecentLabel.setVisibility(View.GONE);
    }

    private void showRecentIfAvailable() {
        if (!recentSearches.isEmpty()) {
            rvRecentSearches.setVisibility(View.VISIBLE);
            tvRecentLabel.setVisibility(View.VISIBLE);
        }
    }

    private void loadRecentSearches() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_RECENT_SEARCHES, "");
        recentSearches.clear();
        if (!json.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    recentSearches.add(arr.getString(i));
                }
            } catch (Exception ignored) {}
        }
        recentAdapter.notifyDataSetChanged();
    }

    private void saveToRecentSearches(String query) {
        recentSearches.remove(query); // move to front if already exists
        recentSearches.add(0, query);
        if (recentSearches.size() > MAX_RECENT) {
            recentSearches.remove(recentSearches.size() - 1);
        }
        JSONArray arr = new JSONArray(recentSearches);
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_RECENT_SEARCHES, arr.toString()).apply();
    }

    private void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && etSearchQuery != null) {
            imm.hideSoftInputFromWindow(etSearchQuery.getWindowToken(), 0);
        }
    }

    private void clearRecentSearches() {
        recentSearches.clear();
        recentAdapter.notifyDataSetChanged();
        hideRecent();
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_RECENT_SEARCHES).apply();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
    }
}
