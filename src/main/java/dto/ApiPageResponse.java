package dto;

import org.springframework.data.domain.Page;

import java.util.List;

public class ApiPageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
    private boolean first;
    private boolean last;

    public static <T> ApiPageResponse<T> from(Page<?> pageData, List<T> content) {
        ApiPageResponse<T> response = new ApiPageResponse<>();
        response.setContent(content);
        response.setPage(pageData.getNumber());
        response.setSize(pageData.getSize());
        response.setTotalPages(pageData.getTotalPages());
        response.setTotalElements(pageData.getTotalElements());
        response.setFirst(pageData.isFirst());
        response.setLast(pageData.isLast());
        return response;
    }

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }

    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }
}
