// ===================================================================
// E-LIBRARY SYSTEM CLIENT LOGIC (Vanilla JS)
// Real-time Countdown Clocks, USN Auth, Role Dashboards,
// Dynamic DBMS Lab Queries Explorer, and Live SQL Workbench Stream
// ===================================================================

let currentUser = null;
let countdownInterval = null;
let lastLoansHash = '';
let lastResHash = '';
let lastCatalogHash = '';
let lastRequestsHash = '';
let lastLibDataHash = '';

document.addEventListener('DOMContentLoaded', () => {
    // Check saved session
    const savedUser = localStorage.getItem('elibrary_user');
    if (savedUser) {
        try {
            currentUser = JSON.parse(savedUser);
            showDashboard();
        } catch (e) {
            localStorage.removeItem('elibrary_user');
            showLoginView();
        }
    } else {
        showLoginView();
    }

    // Set up top bar listeners
    document.getElementById('logout-btn').addEventListener('click', logout);
    document.getElementById('demo-tools-btn').addEventListener('click', toggleDemoModal);

    // Start 1-second interval for real-time countdown clocks
    if (!countdownInterval) {
        countdownInterval = setInterval(updateCountdowns, 1000);
    }

    // Start background live auto-sync across devices (phones and laptop)
    startLiveSync();
});

// ===================================================================
// Authentication (USN & Password)
// ===================================================================
function quickLogin(userId) {
    fetch('/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: userId.toString() })
    })
    .then(res => res.json())
    .then(data => {
        if (data.error) {
            showAlert(data.error, 'error');
            return;
        }
        currentUser = data;
        localStorage.setItem('elibrary_user', JSON.stringify(data));
        showAlert(`Signed in as ${data.name} (USN: ${data.usn})`, 'success');
        showDashboard();
    })
    .catch(err => showAlert('Login failed: ' + err.message, 'error'));
}

function handleCustomLogin(event) {
    event.preventDefault();
    const usn = document.getElementById('login-usn').value.trim();
    const password = document.getElementById('login-password').value.trim();

    fetch('/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ usn, password })
    })
    .then(res => res.json())
    .then(data => {
        if (data.error) {
            showAlert(data.error, 'error');
            return;
        }
        currentUser = data;
        localStorage.setItem('elibrary_user', JSON.stringify(data));
        showAlert(`Signed in as ${data.name}`, 'success');
        showDashboard();
    })
    .catch(err => showAlert('Login failed: ' + err.message, 'error'));
}

function logout() {
    currentUser = null;
    lastLoansHash = '';
    lastResHash = '';
    lastCatalogHash = '';
    lastRequestsHash = '';
    lastLibDataHash = '';
    localStorage.removeItem('elibrary_user');
    showAlert('Logged out successfully', 'success');
    showLoginView();
}

function showLoginView() {
    document.getElementById('login-view').classList.remove('hidden');
    document.getElementById('student-view').classList.add('hidden');
    document.getElementById('librarian-view').classList.add('hidden');
    document.getElementById('user-badge').classList.add('hidden');
}

function showDashboard() {
    lastLoansHash = '';
    lastResHash = '';
    lastCatalogHash = '';
    lastRequestsHash = '';
    lastLibDataHash = '';
    document.getElementById('login-view').classList.add('hidden');
    document.getElementById('user-badge').classList.remove('hidden');
    
    // Update navbar user pill & USN
    const rolePill = document.getElementById('user-role-pill');
    rolePill.textContent = currentUser.role;
    if (currentUser.role === 'LIBRARIAN') {
        rolePill.className = 'role-pill librarian';
    } else {
        rolePill.className = 'role-pill';
    }
    document.getElementById('user-usn').textContent = currentUser.usn;
    document.getElementById('user-name').textContent = currentUser.name;

    if (currentUser.role === 'LIBRARIAN') {
        document.getElementById('student-view').classList.add('hidden');
        document.getElementById('librarian-view').classList.remove('hidden');
        loadLibrarianDashboard();
    } else {
        document.getElementById('librarian-view').classList.add('hidden');
        document.getElementById('student-view').classList.remove('hidden');
        loadStudentDashboard();
    }
}

// ===================================================================
// STUDENT DASHBOARD LOGIC (View-Only; Counter Collection)
// ===================================================================

function loadStudentDashboard() {
    if (!currentUser) return;

    fetch(`/api/me?userId=${currentUser.userId}`)
    .then(res => res.json())
    .then(data => {
        if (data.error) {
            showAlert(data.error, 'error');
            return;
        }

        const user = data.user;
        document.getElementById('student-welcome-name').textContent = `Welcome, ${user.name}`;
        document.getElementById('student-usn-display').textContent = user.usn;
        document.getElementById('student-branch-display').textContent = user.branchCode || 'General';
        document.getElementById('student-sem-display').textContent = user.semester || '5';
        document.getElementById('stat-borrowed-count').textContent = `${data.activeLoans.length} / 5`;
        document.getElementById('stat-reserved-count').textContent = data.pendingReservations.length;

        // Fines Display (Counter-only settlement by Librarian)
        const fineAmount = document.getElementById('stat-fine-amount');
        if (data.totalUnpaidFine > 0) {
            fineAmount.textContent = `₹${data.totalUnpaidFine.toFixed(2)}`;
            fineAmount.className = 'stat-value fine-alert';
        } else {
            fineAmount.textContent = `₹0.00`;
            fineAmount.className = 'stat-value fine-clean';
        }

        // Reservation Penalty / Overdue Warning Banner
        const penaltyBanner = document.getElementById('student-penalty-banner');
        if (user.isReservationBlocked) {
            penaltyBanner.className = 'penalty-banner';
            penaltyBanner.classList.remove('hidden');
            penaltyBanner.innerHTML = `⚠️ <strong>Reservation Suspended (1-Day Penalty):</strong> You failed to physically pick up a reserved book within 12 hours. You cannot reserve books until: <strong>${user.reservationBlockedUntil}</strong>.`;
        } else if (data.hasOverdueLoans && data.totalUnpaidFine > 0) {
            penaltyBanner.className = 'penalty-banner';
            penaltyBanner.classList.remove('hidden');
            penaltyBanner.innerHTML = `🚨 <strong>Overdue Alert & Borrowing Lock:</strong> You have an overdue book with a fine of <strong>₹${data.totalUnpaidFine.toFixed(2)}</strong>! Please visit the library counter to return the book. You can either <em>pay the fine and return the book</em>, or <em>return the book now and pay the fine later</em> at the desk.`;
        } else if (data.hasOverdueLoans) {
            penaltyBanner.className = 'penalty-banner';
            penaltyBanner.classList.remove('hidden');
            penaltyBanner.innerHTML = `🚨 <strong>Overdue Alert:</strong> You have an overdue book. Please visit the library counter immediately to return the physical book.`;
        } else if (data.totalUnpaidFine > 0) {
            penaltyBanner.className = 'penalty-banner';
            penaltyBanner.classList.remove('hidden');
            penaltyBanner.innerHTML = `⚠️ <strong>Unpaid Fine Lock:</strong> You have an outstanding fine of <strong>₹${data.totalUnpaidFine.toFixed(2)}</strong> from a previously returned book. Please pay the librarian at the counter to unblock borrowing privileges.`;
        } else {
            penaltyBanner.className = 'penalty-banner hidden';
        }

        // Render Borrowed Books (30-day live countdowns)
        renderStudentLoans(data.activeLoans);

        // Render 12-Hour Reservations
        renderStudentReservations(data.pendingReservations);

        // Load Books Catalog with Filters
        applyCatalogFilters();

        // Load Community Book Requests
        loadBookRequests();
    })
    .catch(err => console.error(err));
}

function getCountdownText(targetEpoch, isReservation) {
    if (!targetEpoch || isNaN(targetEpoch)) return 'Calculating...';
    const diff = targetEpoch - Date.now();
    if (diff > 0) {
        const days = Math.floor(diff / (1000 * 60 * 60 * 24));
        const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((diff % (1000 * 60)) / 1000);

        if (isReservation) {
            return `⏳ ${hours}h ${minutes}m ${seconds}s left`;
        } else {
            return `⏱️ ${days}d ${hours}h ${minutes}m ${seconds}s`;
        }
    } else {
        if (isReservation) {
            return `🚫 EXPIRED (Missed 12h Pickup)`;
        } else {
            const overdueDays = Math.max(1, Math.floor(Math.abs(diff) / (1000 * 60 * 60 * 24)));
            const fine = overdueDays * 1.00;
            return `⚠️ OVERDUE by ${overdueDays}d | Fine: ₹${fine.toFixed(2)} (Counter Return Required)`;
        }
    }
}

function renderStudentLoans(loans) {
    const container = document.getElementById('my-loans-container');
    if (!loans || loans.length === 0) {
        container.innerHTML = `<div class="empty-state">You currently have no borrowed books. Browse the catalog below to reserve one!</div>`;
        return;
    }

    container.innerHTML = loans.map(loan => `
        <div class="card ${loan.isOverdue ? 'danger-border' : ''}">
            <div class="flex-between">
                <span class="badge ${loan.isOverdue ? 'badge-danger' : 'badge-success'}">${loan.isOverdue ? 'OVERDUE' : 'BORROWED'}</span>
                <span class="text-sm">Copy: <strong>${loan.copyCode}</strong> (${loan.branchCode})</span>
            </div>
            <h4 style="margin: 0.5rem 0 0.25rem 0; font-size: 1.05rem;">${loan.bookTitle}</h4>
            <p class="text-sm">Borrowed: ${loan.borrowDate}</p>
            <p class="text-sm">Due: ${loan.dueDate} (30 Days)</p>
            
            <div style="margin-top: 0.85rem; padding-top: 0.75rem; border-top: 1px solid var(--border-color);">
                <label style="font-size: 0.7rem;">Time Remaining (1-Month Due Countdown):</label>
                <div class="timer-badge countdown-timer ${loan.isOverdue ? 'overdue' : 'active-tick'}" 
                     data-epoch="${loan.dueEpoch}" 
                     data-isoverdue="${loan.isOverdue}" 
                     data-fine="${loan.calculatedFine}">
                     ${getCountdownText(loan.dueEpoch, false)}
                </div>
            </div>
        </div>
    `).join('');
}

function renderStudentReservations(reservations) {
    const container = document.getElementById('my-reservations-container');
    if (!reservations || reservations.length === 0) {
        container.innerHTML = `<div class="empty-state">No active reservations. When you reserve a book, it will hold for 12 hours here.</div>`;
        return;
    }

    container.innerHTML = reservations.map(res => `
        <div class="card">
            <div class="flex-between">
                <span class="badge badge-warning">RESERVED (12h Window)</span>
                <span class="text-sm">Copy: <strong>${res.copyCode}</strong></span>
            </div>
            <h4 style="margin: 0.5rem 0 0.25rem 0; font-size: 1.05rem;">${res.bookTitle}</h4>
            <p class="text-sm">Reserved At: ${res.reservedAt}</p>
            <p class="text-sm">Expires At: ${res.expiresAt}</p>
            
            <div style="margin-top: 0.85rem; padding-top: 0.75rem; border-top: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem;">
                <div>
                    <label style="font-size: 0.7rem;">Physical Pickup Window Left:</label>
                    <div class="timer-badge res-timer countdown-timer" data-epoch="${res.expiresEpoch}" data-isreservation="true">
                        ${getCountdownText(res.expiresEpoch, true)}
                    </div>
                </div>
                <button class="btn btn-sm btn-outline-danger" onclick="cancelReservation(${res.reservationId})">Cancel</button>
            </div>
        </div>
    `).join('');
}

function cancelReservation(reservationId) {
    if (!confirm('Cancel this reservation?')) return;
    fetch('/api/cancel-reservation', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: currentUser.userId.toString(), reservationId: reservationId.toString() })
    })
    .then(res => res.json())
    .then(data => {
        if (data.error) showAlert(data.error, 'error');
        else {
            showAlert('Reservation cancelled.', 'success');
            loadStudentDashboard();
        }
    });
}

// Multi-Level Catalog Filters
function applyCatalogFilters(force = false, isSync = false) {
    const branch = document.getElementById('filter-branch') ? document.getElementById('filter-branch').value : 'ALL';
    const genre = document.getElementById('filter-genre') ? document.getElementById('filter-genre').value : 'ALL';
    const minRating = document.getElementById('filter-rating') ? document.getElementById('filter-rating').value : '0';
    const search = document.getElementById('catalog-search') ? document.getElementById('catalog-search').value.trim() : '';

    let url = `/api/books?branch=${encodeURIComponent(branch)}&genre=${encodeURIComponent(genre)}&minRating=${encodeURIComponent(minRating)}&search=${encodeURIComponent(search)}`;
    if (isSync) {
        url += '&sync=true';
    }

    fetch(url)
    .then(res => res.json())
    .then(books => {
        const catalogHash = JSON.stringify(books);
        if (force || catalogHash !== lastCatalogHash) {
            lastCatalogHash = catalogHash;
            renderCatalog(books);
        }
    });
}

function renderCatalog(books) {
    const container = document.getElementById('books-catalog-container');
    if (!books || books.length === 0) {
        container.innerHTML = `<div class="empty-state">No books match your selected branch/genre/rating criteria.</div>`;
        return;
    }

    container.innerHTML = books.map(book => {
        const isAvailable = book.availableCopies > 0;
        return `
            <div class="book-card">
                <div>
                    <div class="flex-between book-card-header">
                        <span class="badge badge-branch">${book.branchCode}</span>
                        <span class="badge badge-rating">⭐ ${book.rating.toFixed(1)}</span>
                    </div>
                    <h4 class="book-title">${book.title}</h4>
                    <p class="book-author">By ${book.author} (${book.edition})</p>
                    <p class="book-meta">Subject: <strong>${book.subjectName}</strong> | ${book.genreName}</p>
                    <p class="book-meta">ISBN: ${book.isbn} | Pub: ${book.publisher}</p>
                    <p class="book-desc">${book.description}</p>
                </div>

                <div class="book-card-footer">
                    <div>
                        <span class="badge ${isAvailable ? 'badge-success' : 'badge-warning'}">
                            ${isAvailable ? `${book.availableCopies} of ${book.totalCopies} Available` : 'All Copies Borrowed'}
                        </span>
                    </div>
                    <div>
                        <button class="btn btn-sm btn-primary" 
                                onclick="reserveBook(${book.bookId})" 
                                ${isAvailable ? '' : 'disabled title="No copies available"'}>
                            Reserve (12h)
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function reserveBook(bookId) {
    if (!currentUser) return;
    fetch('/api/reserve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: currentUser.userId.toString(), bookId: bookId.toString() })
    })
    .then(res => res.json())
    .then(data => {
        if (data.error) {
            showAlert(data.error, 'error');
        } else {
            showAlert('🎉 Book reserved! You have 12 hours to pick it up at the library counter.', 'success');
            loadStudentDashboard();
        }
    })
    .catch(err => showAlert('Error: ' + err.message, 'error'));
}

// ===================================================================
// Missing Book Requests, Upvoting, and UNVOTING
// ===================================================================
function loadBookRequests(force = false) {
    fetch('/api/requests')
    .then(res => res.json())
    .then(requests => {
        const reqHash = JSON.stringify(requests);
        if (force || reqHash !== lastRequestsHash) {
            lastRequestsHash = reqHash;
            const container = document.getElementById('requests-list-container');
            if (!container) return;
            if (!requests || requests.length === 0) {
                container.innerHTML = `<div class="empty-state">No book requests yet. Suggest one!</div>`;
                return;
            }

            container.innerHTML = requests.map(req => {
                const hasVoted = currentUser && req.votedUserIds.includes(currentUser.userId);
                const isProcured = req.status === 'PROCURED';

                return `
                    <div class="request-item">
                        <div>
                            <strong>${req.title}</strong>
                            <div class="text-sm">Author: ${req.author} | Dept: <strong>${req.branchCode}</strong> (${req.genreName})</div>
                            <div class="text-sm" style="color: var(--text-muted);">Suggested by: ${req.suggestedByName} | Status: <span class="badge ${isProcured ? 'badge-success' : 'badge-warning'}">${req.status}</span></div>
                        </div>
                        <div style="display: flex; align-items: center; gap: 0.5rem;">
                            <span class="votes-count-badge">👍 ${req.votesCount}</span>
                            ${currentUser.role === 'STUDENT' && !isProcured ? (
                                hasVoted 
                                    ? `<button class="btn btn-sm btn-outline-danger" onclick="unvoteRequest(${req.requestId})">👎 Remove Vote</button>`
                                    : `<button class="btn btn-sm btn-outline-primary" onclick="voteRequest(${req.requestId})">👍 +1 Vote</button>`
                            ) : ''}
                        </div>
                    </div>
                `;
            }).join('');
        }
    });
}

function handleNewBookRequest(event) {
    event.preventDefault();
    const title = document.getElementById('req-title').value.trim();
    const author = document.getElementById('req-author').value.trim();
    const branchCode = document.getElementById('req-branch').value;
    const genreName = document.getElementById('req-genre').value;

    fetch('/api/request-book', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            userId: currentUser.userId.toString(),
            title, author, branchCode, genreName
        })
    })
    .then(res => res.json())
    .then(data => {
        if (data.error) showAlert(data.error, 'error');
        else {
            showAlert('Acquisition request submitted!', 'success');
            document.getElementById('new-book-request-form').reset();
            loadBookRequests();
        }
    });
}

function voteRequest(requestId) {
    fetch('/api/vote-request', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: currentUser.userId.toString(), requestId: requestId.toString() })
    })
    .then(res => res.json())
    .then(data => {
        if (data.error) showAlert(data.error, 'error');
        else {
            showAlert('Upvote recorded!', 'success');
            loadBookRequests();
        }
    });
}

function unvoteRequest(requestId) {
    fetch('/api/unvote-request', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: currentUser.userId.toString(), requestId: requestId.toString() })
    })
    .then(res => res.json())
    .then(data => {
        if (data.error) showAlert(data.error, 'error');
        else {
            showAlert('Upvote removed!', 'success');
            loadBookRequests();
        }
    });
}

// ===================================================================
// LIBRARIAN DASHBOARD LOGIC
// ===================================================================
function loadLibrarianDashboard() {
    fetch('/api/librarian-data')
    .then(res => res.json())
    .then(data => {
        // Total Fine Revenue Widget
        document.getElementById('librarian-total-revenue').textContent = `₹${data.totalFinesCollected.toFixed(2)}`;

        // High Demand Books
        renderHighDemand(data.highDemandBooks);

        // Pending 12-Hour Reservations
        renderLibrarianReservations(data.pendingReservations);

        // Populate Walk-in Issue Form
        populateIssueDropdowns(data.students);

        // Active Loans & Returns
        renderLibrarianLoans(data.activeLoans);

        // Unpaid Fines Direct Collection Table
        renderLibrarianFines(data.unpaidFines);

        // Community Requests
        loadLibrarianRequests();
    });
}

function renderHighDemand(highDemandBooks) {
    const container = document.getElementById('high-demand-container');
    if (!highDemandBooks || highDemandBooks.length === 0) {
        container.innerHTML = `<div class="empty-state">No books currently flagged as high demand.</div>`;
        return;
    }

    container.innerHTML = highDemandBooks.map(b => `
        <div class="high-demand-card">
            <div class="flex-between">
                <span class="badge badge-warning">HIGH DEMAND</span>
                <span class="badge badge-branch">${b.branchCode}</span>
            </div>
            <h4 style="margin: 0.5rem 0 0.25rem 0; font-size: 1rem;">${b.title}</h4>
            <p class="text-sm">Author: ${b.author} | Rating: ⭐ ${b.rating}</p>
            <p class="text-sm">Available: <strong>${b.availableCopies} of ${b.totalCopies}</strong></p>
            <p class="text-sm" style="color: #fb923c; margin-top: 0.5rem;">Action: Recommend procuring 2 more physical copies.</p>
        </div>
    `).join('');
}

function renderLibrarianReservations(reservations) {
    const tbody = document.getElementById('librarian-reservations-tbody');
    if (!reservations || reservations.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No students currently waiting on 12-hour reservations.</td></tr>`;
        return;
    }

    tbody.innerHTML = reservations.map(r => `
        <tr>
            <td>#${r.reservationId}</td>
            <td><strong>${r.studentName}</strong><br><small class="text-muted">USN: <code>${r.studentUsn}</code></small></td>
            <td>${r.bookTitle}</td>
            <td><code>${r.copyCode}</code></td>
            <td>${r.reservedAt}</td>
            <td>
                <span class="timer-badge res-timer countdown-timer" data-epoch="${r.expiresEpoch}" data-isreservation="true">
                    ${getCountdownText(r.expiresEpoch, true)}
                </span>
            </td>
            <td>
                <button class="btn btn-sm btn-primary" onclick="approveReservationBorrow(${r.userId}, ${r.copyId})">
                    ✅ Issue to Student
                </button>
            </td>
        </tr>
    `).join('');
}

function populateIssueDropdowns(students) {
    const studentSelect = document.getElementById('issue-student-select');
    if (!studentSelect) return;
    const currentStudent = studentSelect.value;
    studentSelect.innerHTML = `<option value="">-- Choose Student --</option>` + 
        students.map(s => {
            let label = `${s.name} (USN: ${s.usn}) [${s.activeLoansCount}/5 books]`;
            if (s.hasOverdue) label += ` [⚠️ OVERDUE FINES]`;
            if (s.isReservationBlocked) label += ` [🚫 RES BLOCKED]`;
            return `<option value="${s.userId}">${label}</option>`;
        }).join('');
    if (currentStudent) studentSelect.value = currentStudent;

    fetch('/api/books')
    .then(res => res.json())
    .then(books => {
        const copySelect = document.getElementById('issue-copy-select');
        if (!copySelect) return;
        const currentCopy = copySelect.value;
        let options = `<option value="">-- Choose Available Copy --</option>`;
        books.forEach(b => {
            b.copies.forEach(c => {
                if (c.status === 'AVAILABLE' || c.status === 'RESERVED') {
                    options += `<option value="${c.copyId}">${b.title} [${b.branchCode}] - Copy: ${c.copyCode} (${c.rackLocation})</option>`;
                }
            });
        });
        copySelect.innerHTML = options;
        if (currentCopy) copySelect.value = currentCopy;
    });
}

function handleDirectIssue(event) {
    event.preventDefault();
    const studentId = document.getElementById('issue-student-select').value;
    const copyId = document.getElementById('issue-copy-select').value;

    fetch('/api/borrow', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            librarianId: currentUser.userId.toString(),
            studentId: studentId.toString(),
            copyId: copyId.toString()
        })
    })
    .then(res => res.json())
    .then(data => {
        if (data.error) {
            showAlert(data.error, 'error');
        } else {
            showAlert('🎉 Book physically issued! 1-Month countdown started.', 'success');
            loadLibrarianDashboard();
        }
    })
    .catch(err => showAlert('Error: ' + err.message, 'error'));
}

function approveReservationBorrow(studentId, copyId) {
    fetch('/api/borrow', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            librarianId: currentUser.userId.toString(),
            studentId: studentId.toString(),
            copyId: copyId.toString()
        })
    })
    .then(res => res.json())
    .then(data => {
        if (data.error) {
            showAlert(data.error, 'error');
        } else {
            showAlert('Reservation approved and book physically borrowed!', 'success');
            loadLibrarianDashboard();
        }
    });
}

function renderLibrarianLoans(loans) {
    const tbody = document.getElementById('librarian-loans-tbody');
    if (!loans || loans.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" class="empty-state">No on-time active loans across campus (any overdue loans are in the Overdue Counter below).</td></tr>`;
        return;
    }

    tbody.innerHTML = loans.map(l => `
        <tr>
            <td>#${l.loanId}</td>
            <td><code>${l.studentUsn}</code></td>
            <td><strong>${l.studentName}</strong></td>
            <td>${l.bookTitle}</td>
            <td><code>${l.copyCode}</code></td>
            <td>${l.borrowDate}</td>
            <td>
                <div class="timer-badge countdown-timer active-tick" 
                     data-epoch="${l.dueEpoch}" 
                     data-isoverdue="false" 
                     data-fine="0.0">
                     Calculating...
                </div>
            </td>
            <td>
                <span class="badge badge-success">ACTIVE (ON-TIME)</span>
            </td>
            <td>
                <button class="btn btn-sm btn-outline-primary" onclick="processReturn(${l.loanId}, false)">
                    🔄 Return Book
                </button>
            </td>
        </tr>
    `).join('');
}

function processReturn(loanId, collectFineNow) {
    const promptMsg = collectFineNow 
        ? 'Option 1: Collect overdue fine and accept physical book return now?' 
        : 'Option 2: Accept physical book return now and allow student to pay fine later?';
    if (!confirm(promptMsg)) return;

    fetch('/api/return', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            librarianId: currentUser.userId.toString(),
            loanId: loanId.toString(),
            collectFineNow: collectFineNow ? "true" : "false"
        })
    })
    .then(res => res.json())
    .then(data => {
        if (data.error) {
            showAlert(data.error, 'error');
        } else {
            if (data.fineCollected) {
                showAlert(`✅ Book returned & fine of ₹${data.fineAmount.toFixed(2)} collected! Student borrowing lock removed.`, 'success');
            } else if (data.fineAmount > 0) {
                showAlert(`⚠️ Book returned to shelf! Overdue fine of ₹${data.fineAmount.toFixed(2)} marked as UNPAID (Pay Later). Student borrowing remains locked until settled.`, 'warning');
            } else {
                showAlert('Book returned on time. No fine assessed.', 'success');
            }
            loadLibrarianDashboard();
        }
    });
}

function renderLibrarianFines(fines) {
    const tbody = document.getElementById('librarian-fines-tbody');
    if (!fines || fines.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" class="empty-state">No overdue loans or outstanding fines pending collection across campus.</td></tr>`;
        return;
    }

    tbody.innerHTML = fines.map(f => {
        const isBorrowed = !f.isBookReturned;
        const idLabel = f.fineId > 0 ? `#F-${f.fineId}` : `#L-${f.loanId}`;

        let statusBadge = '';
        let amountDisplay = `<strong style="color: var(--accent-danger);">₹${f.amount.toFixed(2)}</strong>`;
        let actionButtons = '';
        let rowClass = 'danger-row';

        if (isBorrowed) {
            // Book is still in student possession (Overdue loan)
            // Librarian has 2 options: Pay & Return, OR Return Now (Pay Later)
            statusBadge = `<span class="badge badge-danger">OVERDUE (BORROWED)</span>`;
            actionButtons = `
                <div style="display: flex; gap: 0.35rem; flex-wrap: wrap;">
                    <button class="btn btn-sm btn-success" onclick="processReturn(${f.loanId}, true)" title="Option 1: Collect fine and return physical book together">
                        💳 Pay & Return Book (₹${f.amount.toFixed(2)})
                    </button>
                    <button class="btn btn-sm btn-outline-warning" onclick="processReturn(${f.loanId}, false)" title="Option 2: Check in physical book now; student pays fine later">
                        ↩️ Return Book (Pay Later)
                    </button>
                </div>
            `;
        } else {
            // Book was physically returned to shelf, but fine is still UNPAID
            rowClass = 'warning-row';
            statusBadge = `<span class="badge badge-warning">BOOK RETURNED (FINE UNPAID)</span>`;
            actionButtons = `
                <button class="btn btn-sm btn-success" onclick="librarianCollectFine(${f.fineId})" title="Collect overdue fine at desk and unlock student borrowing privileges">
                    💳 Collect Fine & Clear Lock (₹${f.amount.toFixed(2)})
                </button>
            `;
        }

        return `
            <tr class="${rowClass}">
                <td><code>${idLabel}</code></td>
                <td><code>${f.studentUsn}</code></td>
                <td><strong>${f.studentName}</strong></td>
                <td>${f.bookTitle}</td>
                <td><code>${f.copyCode || 'N/A'}</code></td>
                <td><span class="badge badge-danger">${f.daysOverdue} days overdue</span></td>
                <td>${amountDisplay}</td>
                <td>${statusBadge}</td>
                <td>${actionButtons}</td>
            </tr>
        `;
    }).join('');
}

function librarianCollectFine(fineId) {
    if (!confirm('Mark fine as collected and clear student borrowing restriction?')) return;
    fetch('/api/collect-fine', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            librarianId: currentUser.userId.toString(),
            fineId: fineId.toString()
        })
    })
    .then(res => res.json())
    .then(data => {
        if (data.error) showAlert(data.error, 'error');
        else {
            showAlert('Fine payment collected! Student lock removed.', 'success');
            loadLibrarianDashboard();
        }
    });
}

function loadLibrarianRequests() {
    fetch('/api/requests')
    .then(res => res.json())
    .then(requests => {
        const tbody = document.getElementById('librarian-requests-tbody');
        if (!requests || requests.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" class="empty-state">No requests submitted yet.</td></tr>`;
            return;
        }

        tbody.innerHTML = requests.map(r => `
            <tr>
                <td>#${r.requestId}</td>
                <td><strong>${r.title}</strong></td>
                <td>${r.author}</td>
                <td><span class="badge badge-branch">${r.branchCode}</span></td>
                <td>${r.suggestedByName}</td>
                <td><span class="badge badge-warning">👍 ${r.votesCount}</span></td>
                <td><span class="badge ${r.status === 'PROCURED' ? 'badge-success' : 'badge-warning'}">${r.status}</span></td>
                <td>
                    ${r.status === 'PENDING' ? `
                        <button class="btn btn-sm btn-primary" onclick="procureBook(${r.requestId})">
                            ✅ Approve & Procure
                        </button>
                    ` : `<span class="text-sm" style="color: var(--accent-green);">Procured & In Catalog</span>`}
                </td>
            </tr>
        `).join('');
    });
}

function procureBook(requestId) {
    if (!confirm('Approve this purchase request and add 2 physical copies to the library catalog?')) return;
    fetch('/api/procure-request', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            librarianId: currentUser.userId.toString(),
            requestId: requestId.toString()
        })
    })
    .then(res => res.json())
    .then(data => {
        if (data.error) showAlert(data.error, 'error');
        else {
            showAlert(`🎉 "${data.bookTitle}" has been procured and added to the catalog with 2 copies!`, 'success');
            loadLibrarianDashboard();
        }
    });
}


// ===================================================================
// LIVE COUNTDOWN TIMER ENGINE (1-Second Interval)
// ===================================================================
function updateCountdowns() {
    const timerElements = document.querySelectorAll('.countdown-timer');
    const now = Date.now();

    timerElements.forEach(el => {
        const targetEpoch = parseInt(el.getAttribute('data-epoch'));
        const isReservation = el.getAttribute('data-isreservation') === 'true';
        if (isNaN(targetEpoch)) return;

        const diff = targetEpoch - now;

        if (diff > 0) {
            const days = Math.floor(diff / (1000 * 60 * 60 * 24));
            const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
            const seconds = Math.floor((diff % (1000 * 60)) / 1000);

            if (isReservation) {
                el.textContent = `⏳ ${hours}h ${minutes}m ${seconds}s left`;
                el.className = 'timer-badge res-timer countdown-timer';
            } else {
                el.textContent = `⏱️ ${days}d ${hours}h ${minutes}m ${seconds}s`;
                el.className = 'timer-badge active-tick countdown-timer';
            }
        } else {
            if (isReservation) {
                el.textContent = `🚫 EXPIRED (Missed 12h Pickup)`;
                el.className = 'timer-badge overdue countdown-timer';
            } else {
                const overdueDays = Math.max(1, Math.floor(Math.abs(diff) / (1000 * 60 * 60 * 24)));
                const fine = overdueDays * 1.00;
                el.textContent = `⚠️ OVERDUE by ${overdueDays}d | Fine: ₹${fine.toFixed(2)} (Counter Return Required)`;
                el.className = 'timer-badge overdue countdown-timer';
            }
        }
    });
}

// ===================================================================
// TEACHER DEMO TOOLS & LIVE SQL STREAM
// ===================================================================
function toggleDemoModal() {
    const modal = document.getElementById('demo-modal');
    modal.classList.toggle('hidden');
    if (!modal.classList.contains('hidden')) {
        loadSqlLogs();
    }
}

function triggerSimulation(type) {
    fetch('/api/simulate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ type })
    })
    .then(res => res.json())
    .then(data => {
        showAlert(data.message, 'success');
        loadSqlLogs();
        if (currentUser && currentUser.role === 'LIBRARIAN') {
            loadLibrarianDashboard();
        } else if (currentUser) {
            loadStudentDashboard();
        }
    });
}

function triggerReset() {
    if (!confirm('Reset all loans, reservations, fines, and transactions to initial state?')) return;
    fetch('/api/reset', { method: 'POST' })
    .then(res => res.json())
    .then(data => {
        showAlert(data.message, 'success');
        loadSqlLogs();
        if (currentUser && currentUser.role === 'LIBRARIAN') {
            loadLibrarianDashboard();
        } else if (currentUser) {
            loadStudentDashboard();
        }
    });
}

function loadSqlLogs() {
    fetch('/api/sql-logs')
    .then(res => res.json())
    .then(logs => {
        const terminal = document.getElementById('sql-log-terminal');
        if (!logs || logs.length === 0) {
            terminal.textContent = '-- No SQL transactions logged yet.';
            return;
        }
        terminal.textContent = logs.join('\n');
    });
}

function clearSqlLogs() {
    fetch('/api/clear-sql-logs', { method: 'POST' })
    .then(res => res.json())
    .then(data => {
        showAlert('SQL logs cleared! In DBeaver/Workbench, right-click file & select Revert/Reload to refresh.', 'success');
        loadSqlLogs();
    })
    .catch(err => showAlert('Error clearing logs: ' + err.message, 'error'));
}

function copySqlLogs() {
    const text = document.getElementById('sql-log-terminal').textContent;
    navigator.clipboard.writeText(text).then(() => {
        showAlert('SQL statements copied to clipboard! Ready to paste into MySQL Workbench.', 'success');
    });
}

function showAlert(message, type = 'info') {
    const alert = document.getElementById('global-alert');
    alert.textContent = message;
    alert.className = `global-alert ${type}`;
    alert.classList.remove('hidden');

    setTimeout(() => {
        alert.classList.add('hidden');
    }, 4500);
}

// ===================================================================
// REAL-TIME AUTO-SYNC (Live multi-device synchronization)
// Automatically synchronizes state between laptop and connected mobile phones
// ===================================================================
let liveSyncInterval = null;
let isSyncing = false;

function startLiveSync() {
    if (liveSyncInterval) clearInterval(liveSyncInterval);
    liveSyncInterval = setInterval(() => {
        if (!currentUser || isSyncing) return;

        // Skip background sync if the user is actively typing or selecting in forms
        const activeTag = document.activeElement ? document.activeElement.tagName.toLowerCase() : '';
        if (activeTag === 'input' || activeTag === 'textarea' || activeTag === 'select') {
            return;
        }

        isSyncing = true;

        if (currentUser.role === 'LIBRARIAN') {
            fetch('/api/librarian-data')
            .then(res => res.json())
            .then(data => {
                const libHash = JSON.stringify(data);
                if (libHash !== lastLibDataHash) {
                    lastLibDataHash = libHash;
                    const revEl = document.getElementById('librarian-total-revenue');
                    if (revEl) revEl.textContent = `₹${data.totalFinesCollected.toFixed(2)}`;
                    renderHighDemand(data.highDemandBooks);
                    renderLibrarianReservations(data.pendingReservations);
                    populateIssueDropdowns(data.students);
                    renderLibrarianLoans(data.activeLoans);
                    renderLibrarianFines(data.unpaidFines);
                    loadLibrarianRequests();
                }
            })
            .catch(err => console.debug('Sync err:', err))
            .finally(() => {
                isSyncing = false;
            });
        } else {
            fetch(`/api/me?userId=${currentUser.userId}`)
            .then(res => res.json())
            .then(data => {
                if (!data || data.error) return;
                const borrowedEl = document.getElementById('stat-borrowed-count');
                const reservedEl = document.getElementById('stat-reserved-count');
                if (borrowedEl) borrowedEl.textContent = `${data.activeLoans.length} / 5`;
                if (reservedEl) reservedEl.textContent = data.pendingReservations.length;

                const fineAmount = document.getElementById('stat-fine-amount');
                if (fineAmount) {
                    if (data.totalUnpaidFine > 0) {
                        fineAmount.textContent = `₹${data.totalUnpaidFine.toFixed(2)}`;
                        fineAmount.className = 'stat-value fine-alert';
                    } else {
                        fineAmount.textContent = `₹0.00`;
                        fineAmount.className = 'stat-value fine-clean';
                    }
                }

                // Diff active loans
                const loansHash = JSON.stringify(data.activeLoans);
                if (loansHash !== lastLoansHash) {
                    lastLoansHash = loansHash;
                    renderStudentLoans(data.activeLoans);
                }

                // Diff pending reservations
                const resHash = JSON.stringify(data.pendingReservations);
                if (resHash !== lastResHash) {
                    lastResHash = resHash;
                    renderStudentReservations(data.pendingReservations);
                }

                // Diff catalog & requests (pass true to avoid spamming SELECT queries on background sync)
                applyCatalogFilters(false, true);
                loadBookRequests(false);
            })
            .catch(err => console.debug('Sync err:', err))
            .finally(() => {
                isSyncing = false;
            });
        }

        // Live SQL stream if demo modal is open
        const demoModal = document.getElementById('demo-modal');
        if (demoModal && !demoModal.classList.contains('hidden')) {
            loadSqlLogs();
        }
    }, 2000); // Polls every 2 seconds for instantaneous multi-device updates
}
