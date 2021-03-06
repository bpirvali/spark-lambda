/*
 * Copyright (c) 2015-2017 Toby Weston
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package learning.chapter12

import java.lang

class CustomersSortableBySpend extends Customers {

  // no longer needed since Scala 2.12
  implicit def functionToOrdering[A](f: (A, A) => Int): Ordering[A] = {
    new Ordering[A] {
      //def compare(a: A, b: A) = f.apply(a, b)
      def compare(a: A, b: A) = f(a, b)
    }
  }

  def sortByTotal =(a: Customer, b: Customer, asc: Boolean) =>
    if (asc) a.total.compare(b.total) else b.total.compare(a.total)

  def sortByName =(a: Customer, b: Customer, asc: Boolean) =>
    if (asc) a.name.compare(b.name) else b.name.compare(a.name)

  override def sort: List[Customer] = {
    this.iterator.toList.sorted((a: Customer, b: Customer) => b.total.compare(a.total))
  }

  def sortBy(f: (Customer, Customer) => Int): List[Customer] = {
    this.iterator.toList.sorted(f)
  }
}
