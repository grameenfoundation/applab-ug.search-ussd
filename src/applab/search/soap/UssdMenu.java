/*
 * Copyright (C) 2011 Grameen Foundation
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package applab.search.soap;

import java.util.ArrayList;
import java.util.Collections;

public class UssdMenu {
    private String title = "";
    private ArrayList<String> items;
    private String breadCrumb = "";
    private Integer categoryId = 0;
    private Integer page = 1;
    final static private Integer pageSize = 8;

    public UssdMenu() {
        this.items = new ArrayList<String>();
    }

    public void addItem(String item) {
        if (item != "") {
            items.add(item.replace("_", " "));
        }
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMenuStringForDisplay() {
        return getMenuString("\r\n", true, UssdMenu.pageSize, this.page);
    }

    public void setPage(Integer page) {
        this.page = Math.max(1, page);
    }

    public String getMenuString(String delimeter, Boolean includeIndices, Integer itemsPerPage, Integer page) {
        // Sort items
        Collections.sort(this.items);
        String menuString = this.title;

        // Add page indicator if necessary
        Double totalPages = this.getTotalNumberOfPages(itemsPerPage);
        if (totalPages > 1) {
            menuString += delimeter + "Page " + page + " of " + totalPages.intValue();
        }

        // Return first numberOfItems items
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = this.items.size();
        int itemCounter;
        int itemDisplayIndex = 1;

        for (itemCounter = startIndex; itemCounter < endIndex; itemCounter++) {
            menuString += delimeter + ((includeIndices) ? (itemDisplayIndex) + ". " : "") + this.items.get(itemCounter);

            if ((itemDisplayIndex == itemsPerPage) && (itemCounter < endIndex)) {

                // We have more items, so add a "More" link instead of this item
                menuString += delimeter + ((includeIndices) ? ((itemDisplayIndex + 1) + ". ") : "") + "Next Page";
                return menuString;
            }
            itemDisplayIndex++;
        }
        return menuString;
    }

    private Double getTotalNumberOfPages(Integer itemsPerPage) {
        return Math.ceil(this.items.size() / (double)itemsPerPage);
    }

    public void unserialize(String string) {
        String[] parts = string.split("##");
        this.setTitle(parts[0]);
        for (int counter = 1; counter < parts.length; counter++) {
            this.addItem(parts[counter]);
        }
    }

    public void setBreadCrumb(String breadCrumb) {
        this.breadCrumb = breadCrumb;
    }

    public void addPathToBreadCrumb(String path) {
        if (this.breadCrumb != "") {
            this.breadCrumb = this.breadCrumb.concat(" ");
        }

        this.breadCrumb = this.breadCrumb.concat(path.replace(" ", "_"));
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getItem(Integer index) {
        Collections.sort(this.items);
        index = (this.page - 1) * UssdMenu.pageSize + index;
        return this.items.get(index);
    }

    public String getBreadCrumb() {
        return this.breadCrumb;
    }

    public Integer getCategoryId() {
        return this.categoryId;
    }

    public Integer getItemCount() {
        return this.items.size();
    }

    public int getPage() {
        return this.page;
    }
}
